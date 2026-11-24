import SwiftUI
import shared

// Sizes of the Compose PttButton and PttScreen, so both lenses look the same
private let regularLensSize: CGFloat = 208
private let compactLensSize: CGFloat = 160
private let compactBelowHeight: CGFloat = 700
private let iconScale: CGFloat = 0.3
private let pressedScale: CGFloat = 0.97
private let ringCount = 3
private let ringSpacing: CGFloat = 16
private let ringBaseOpacity = 0.4
private let requestRingGap: CGFloat = 10
private let requestRingStroke: CGFloat = 2
private let requestRingDash: [CGFloat] = [10, 8]
private let glowScale: CGFloat = 2.5
private let idleGlowIntensity = 0.18
private let tintedGlowIntensity = 0.5
private let toastDuration: Duration = .seconds(3)
/// Same pace as the Compose `animate*AsState` defaults on the button and avatars.
private let stateAnimation = Animation.easeOut(duration: 0.3)
// Participant avatars
private let avatarSize: CGFloat = 48
private let avatarColumnWidth: CGFloat = 64
private let avatarHaloWidth: CGFloat = 4
private let avatarHighlightOpacity = 0.22

/// State and floor-denied effect of the `PttComponent`, kept while the screen is shown.
final class PttModel: ObservableObject {
    let component: PttComponent
    @Published private(set) var state: PttState
    @Published private(set) var badge: Core_designsystemConnectionStatus
    @Published private(set) var isCacheEnabled: Bool
    @Published var toast: String?
    private var observations: [Observation] = []

    init(component: PttComponent, root: RootComponent) {
        self.component = component
        state = component.state.value as! PttState
        badge = ConnectionBadgeKt.badgeStatus(status: root.connectionStatus.value as! ConnectionStatus)
        isCacheEnabled = (root.isCacheEnabled.value as? Bool) ?? false
        // Back and history are collected by the root, as with the Compose screen; only the denial is shown here
        observations = [
            SwiftBridgeKt.observe(flow: component.state) { [weak self] value in
                if let state = value as? PttState { self?.state = state }
            },
            SwiftBridgeKt.observe(flow: root.connectionStatus) { [weak self] value in
                if let status = value as? ConnectionStatus { self?.badge = ConnectionBadgeKt.badgeStatus(status: status) }
            },
            SwiftBridgeKt.observe(flow: root.isCacheEnabled) { [weak self] value in
                self?.isCacheEnabled = (value as? Bool) ?? false
            },
            SwiftBridgeKt.observe(flow: component.effects) { [weak self] effect in
                guard let denied = effect as? PttEffectShowFloorDenied else { return }
                Task { @MainActor in
                    // The server's reason is protocol text, shown as it comes (CLAUDE.md)
                    var message = denied.serverReason ?? ""
                    if denied.serverReason == nil {
                        message = await Texts.format("ptt_channel_busy", [])
                    }
                    self?.show(toast: message)
                }
            },
        ]
    }

    deinit {
        observations.forEach { $0.cancel() }
    }

    func send(_ intent: PttIntent) {
        component.onIntent(intent: intent)
    }

    @MainActor
    private func show(toast message: String) {
        toast = message
        Task { @MainActor [weak self] in
            try? await Task.sleep(for: toastDuration)
            if self?.toast == message { self?.toast = nil }
        }
    }
}

/// PTT screen in SwiftUI (32.4), over the same `PttComponent` as the Compose one. The lens is Liquid Glass.
struct PttView: View {
    let root: RootComponent
    /// Beside the channel list (iPad), whose back disconnects: this one shows as "close" so the two are not confused (29.3)
    let besideChannels: Bool
    @StateObject private var model: PttModel
    @StateObject private var texts = Texts(["ptt_back_to_channels", "ptt_close_channel", "common_history", "ptt_leave", "ptt_in_channel"])
    @State private var announcedCount: Int?
    @Environment(\.pttTheme) private var theme

    init(component: PttComponent, root: RootComponent, besideChannels: Bool = false) {
        self.root = root
        self.besideChannels = besideChannels
        _model = StateObject(wrappedValue: PttModel(component: component, root: root))
    }

    private var state: PttState { model.state }
    private var buttonState: Core_designsystemPttButtonState { state.buttonState() }

    var body: some View {
        Group {
            GeometryReader { geometry in
                VStack(spacing: 0) {
                    Spacer(minLength: 0)
                    StatusBlock(copy: state.statusCopy(), speakerName: state.currentSpeakerName)
                    PttLens(
                        state: buttonState,
                        size: geometry.size.height < compactBelowHeight ? compactLensSize : regularLensSize,
                        onPress: { model.send(PttIntentPressPtt.shared) },
                        onRelease: { model.send(PttIntentReleasePtt.shared) }
                    )
                    .padding(.vertical, PttTheme.dimens.spaceXl)
                    ResourceText(resource: state.hint())
                        .font(.subheadline)
                        .foregroundStyle(theme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, PttTheme.dimens.space2xl)
                    Spacer(minLength: 0)
                    participants
                    Button(texts["ptt_leave"], systemImage: "rectangle.portrait.and.arrow.right") {
                        model.send(PttIntentLeaveChannel.shared)
                    }
                    .foregroundStyle(theme.statusOffline)
                    .modifier(GlassButtonStyle())
                    .controlSize(.large)
                    .padding(.vertical, PttTheme.dimens.spaceXl)
                    .accessibilityIdentifier("leaveChannel")
                }
                .frame(maxWidth: .infinity)
            }
            .background(theme.background.ignoresSafeArea())
            .overlay(alignment: .top) { toast }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(
                        texts[besideChannels ? "ptt_close_channel" : "ptt_back_to_channels"],
                        systemImage: besideChannels ? "xmark" : "chevron.backward"
                    ) { root.goBack() }
                        .accessibilityIdentifier("backToChannels")
                }
                ToolbarItem(placement: .principal) {
                    ChannelTitle(channelId: state.channelId, count: state.participants.count)
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    // Only while the connection is not healthy: the channel title owns the center
                    if model.badge != .online {
                        StatusBadge(status: model.badge)
                    }
                    if model.isCacheEnabled {
                        Button(texts["common_history"], systemImage: "clock.arrow.circlepath") {
                            model.send(PttIntentGoToHistory.shared)
                        }
                    }
                }
            }
        }
    }

    private var participants: some View {
        VStack(spacing: PttTheme.dimens.spaceLg) {
            HStack {
                Text(texts["ptt_in_channel"].uppercased())
                Spacer()
                Text("\(state.participants.count)")
            }
            .font(.caption.weight(.semibold))
            .foregroundStyle(theme.textTertiary)
            .padding(.horizontal, PttTheme.dimens.space2xl)
            .accessibilityElement(children: .combine)
            // Announced when someone joins or leaves, like the live region of the Compose count (29.4)
            .task(id: state.participants.count) {
                let count = state.participants.count
                defer { announcedCount = count }
                guard announcedCount != nil else { return } // the count on opening is read with the screen, not announced
                UIAccessibility.post(notification: .announcement, argument: await Texts.format("ptt_participants", [count]))
            }
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: PttTheme.dimens.spaceXs) {
                    ForEach(state.participants, id: \.userId) { participant in
                        ParticipantAvatar(participant: participant, state: state, buttonState: buttonState)
                    }
                }
                .padding(.horizontal, PttTheme.dimens.spaceXl)
            }
        }
        .padding(.vertical, PttTheme.dimens.spaceLg)
        .glassControl(in: RoundedRectangle(cornerRadius: PttTheme.dimens.panelRadius, style: .continuous))
        .padding(.horizontal, PttTheme.dimens.spaceXl)
    }

    @ViewBuilder
    private var toast: some View {
        if let message = model.toast {
            Text(message)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(theme.textPrimary)
                .padding(.horizontal, PttTheme.dimens.spaceXl)
                .padding(.vertical, PttTheme.dimens.spaceLg)
                .glassControl(in: Capsule())
                .padding(.top, PttTheme.dimens.spaceSm)
                .transition(.move(edge: .top).combined(with: .opacity))
                .accessibilityIdentifier("floorDenied")
        }
    }
}

/// Colored eyebrow and big headline above the lens; the copy and its tone come from `statusCopy()` in Kotlin.
private struct StatusBlock: View {
    let copy: StatusCopy
    let speakerName: String?
    @State private var eyebrow = ""
    @State private var headline = ""
    @Environment(\.pttTheme) private var theme

    var body: some View {
        VStack(spacing: PttTheme.dimens.spaceXs) {
            HStack(spacing: PttTheme.dimens.spaceSm) {
                Circle().fill(color).frame(width: PttTheme.dimens.spaceMd, height: PttTheme.dimens.spaceMd)
                Text(eyebrow.uppercased()).font(.caption.weight(.semibold)).foregroundStyle(color)
            }
            Text(headline)
                .font(.largeTitle.weight(.semibold))
                .foregroundStyle(theme.textPrimary)
                .multilineTextAlignment(.center)
                .lineLimit(2)
        }
        .padding(.horizontal, PttTheme.dimens.space2xl)
        .accessibilityElement(children: .combine)
        .task(id: "\(copy.tone)-\(speakerName ?? "")") {
            eyebrow = (try? await SwiftBridgeKt.text(resource: copy.eyebrow, args: [])) ?? ""
            var args: [Any] = []
            if copy.namesSpeaker {
                if let speakerName {
                    args = [speakerName]
                } else {
                    args = [await Texts.format("ptt_someone", [])]
                }
            }
            headline = (try? await SwiftBridgeKt.text(resource: copy.headline, args: args)) ?? ""
            // Announced when it changes, like the live region of the Compose status (29.4)
            UIAccessibility.post(notification: .announcement, argument: "\(eyebrow), \(headline)")
        }
    }

    private var color: Color {
        switch copy.tone {
        case .own: theme.accentTx
        case .receiving: theme.primary
        case .offline: theme.statusIdle
        default: theme.statusOnline
        }
    }
}

/// The push-to-talk lens (ADR 0006): clear glass when free, amber while transmitting, blue while someone else
/// speaks, and a dashed amber ring while asking for the floor. Hold to talk, release to stop.
private struct PttLens: View {
    let state: Core_designsystemPttButtonState
    let size: CGFloat
    let onPress: () -> Void
    let onRelease: () -> Void
    @State private var isHeld = false
    @State private var label = ""
    @State private var actionLabel = ""
    @Environment(\.pttTheme) private var theme

    private var isTinted: Bool { state == .transmitting || state == .receiving }
    private var tint: Color { state == .receiving ? theme.primary : theme.accentTx }

    var body: some View {
        ZStack {
            AmbientGlow(color: isTinted ? tint : theme.primary, intensity: isTinted ? tintedGlowIntensity : idleGlowIntensity, size: size * glowScale)
            ForEach(1...ringCount, id: \.self) { ring in
                Circle()
                    .stroke(tint.opacity(isTinted ? ringBaseOpacity / Double(ring) : 0), lineWidth: 1)
                    .frame(width: size + 2 * ringSpacing * CGFloat(ring), height: size + 2 * ringSpacing * CGFloat(ring))
            }
            Circle()
                .stroke(theme.accentTx.opacity(state == .requesting ? 1 : 0), style: StrokeStyle(lineWidth: requestRingStroke, dash: requestRingDash))
                .frame(width: size + 2 * requestRingGap, height: size + 2 * requestRingGap)
            Image(systemName: state == .receiving ? "waveform" : "mic.fill")
                .font(.system(size: size * iconScale, weight: .medium))
                .foregroundStyle(iconColor)
                .frame(width: size, height: size)
                .modifier(LensSurface(tint: isTinted ? tint : nil))
                .scaleEffect(state == .transmitting || state == .requesting ? pressedScale : 1)
        }
        .frame(width: size + 2 * ringSpacing * CGFloat(ringCount), height: size + 2 * ringSpacing * CGFloat(ringCount))
        .contentShape(Circle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in
                    guard !isHeld else { return }
                    isHeld = true
                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    onPress()
                }
                .onEnded { _ in
                    isHeld = false
                    UIImpactFeedbackGenerator(style: .light).impactOccurred()
                    onRelease()
                }
        )
        .animation(stateAnimation, value: state)
        .accessibilityElement()
        .accessibilityLabel(label)
        .accessibilityHint(actionLabel)
        .accessibilityAddTraits(.isButton)
        // VoiceOver's double tap cannot hold: it toggles the talk instead (29.4)
        .accessibilityAction {
            if SwiftBridgeKt.pttScreenReaderStartsTalking(state: state) {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                onPress()
            } else {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                onRelease()
            }
        }
        .accessibilityIdentifier("ptt")
        .task(id: state) {
            label = (try? await SwiftBridgeKt.text(resource: SwiftBridgeKt.pttButtonLabel(state: state), args: [])) ?? ""
            let starts = SwiftBridgeKt.pttScreenReaderStartsTalking(state: state)
            actionLabel = await Texts.format(starts ? "ptt_action_start" : "ptt_action_stop", [])
        }
    }

    private var iconColor: Color {
        switch state {
        case .requesting: theme.accentTx
        case .transmitting: theme.onAccentTx
        case .receiving: theme.onPrimary
        default: theme.textPrimary
        }
    }
}

/// The lens itself: interactive Liquid Glass on iOS 26, tinted while someone talks; a plain disc before it and with
/// "Reduzir transparência".
private struct LensSurface: ViewModifier {
    let tint: Color?
    @Environment(\.pttTheme) private var theme

    func body(content: Content) -> some View {
        if #available(iOS 26, *), !theme.reduceTransparency {
            content.glassEffect(.regular.tint(tint).interactive(), in: Circle())
        } else {
            content.background(tint ?? theme.surface, in: Circle()).overlay(Circle().stroke(theme.border, lineWidth: 1))
        }
    }
}

/// Channel name and head count in a glass capsule, at the center of the bar.
private struct ChannelTitle: View {
    let channelId: String
    let count: Int
    @State private var title = ""
    @State private var subtitle = ""
    @Environment(\.pttTheme) private var theme

    var body: some View {
        VStack(spacing: 0) {
            Text(title).font(.headline).foregroundStyle(theme.textPrimary).lineLimit(1)
            Text(subtitle.uppercased()).font(.caption2.weight(.semibold)).foregroundStyle(theme.textTertiary)
        }
        .task(id: "\(channelId)-\(count)") {
            title = await Texts.format("channel_name", [channelId])
            subtitle = await Texts.format("ptt_participants", [count])
        }
    }
}

/// Round avatar with the name below: blue ring when someone else speaks, amber when it is the user's own turn.
private struct ParticipantAvatar: View {
    let participant: ParticipantDomain
    let state: PttState
    let buttonState: Core_designsystemPttButtonState
    @State private var selfName = ""
    @Environment(\.pttTheme) private var theme

    private var isSelf: Bool { participant.userId == state.localUserId }
    private var isSpeaking: Bool { participant.userId == state.currentSpeakerId }

    private var highlight: Color? {
        if isSpeaking { return isSelf ? theme.accentTx : theme.primary }
        if isSelf && buttonState == .requesting { return theme.accentTx }
        return nil
    }

    var body: some View {
        VStack(spacing: PttTheme.dimens.spaceSm) {
            Text(participant.nickname.prefix(1).uppercased())
                .font(.headline)
                .foregroundStyle(theme.textPrimary)
                .frame(width: avatarSize, height: avatarSize)
                .background(highlight?.opacity(avatarHighlightOpacity) ?? theme.surface, in: Circle())
                .overlay(Circle().stroke(highlight ?? theme.border, lineWidth: 1))
                .background(
                    Circle()
                        .fill(highlight?.opacity(avatarHighlightOpacity) ?? .clear)
                        .padding(-avatarHaloWidth)
                )
            Text(isSelf ? selfName : participant.nickname)
                .font(.caption)
                .fontWeight(highlight != nil ? .semibold : .regular)
                .foregroundStyle(highlight ?? theme.textSecondary)
                .lineLimit(1)
        }
        .frame(width: avatarColumnWidth)
        .animation(stateAnimation, value: highlight)
        .task { selfName = await Texts.format("participant_self", []) }
    }
}
