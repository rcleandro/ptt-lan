import SwiftUI
import shared

// Ambient light, the same as the Compose screen: green from the top corner, blue from the bottom one
private let topGlowSize: CGFloat = 440
private let topGlowOffset = CGSize(width: 160, height: -120)
private let topGlowIntensity = 0.22
private let bottomGlowSize: CGFloat = 420
private let bottomGlowOffset = CGSize(width: -180, height: 80)
private let bottomGlowIntensity = 0.2

/// State of the `ChannelListComponent` and the connection badge, kept while the screen is shown.
final class ChannelListModel: ObservableObject {
    let component: ChannelListComponent
    @Published private(set) var state: ChannelListState
    @Published private(set) var badge: Core_designsystemConnectionStatus
    @Published private(set) var isCacheEnabled: Bool
    private var observations: [Observation] = []

    init(component: ChannelListComponent, root: RootComponent) {
        self.component = component
        state = component.state.value as! ChannelListState
        badge = ConnectionBadgeKt.badgeStatus(status: root.connectionStatus.value as! ConnectionStatus)
        isCacheEnabled = (root.isCacheEnabled.value as? Bool) ?? false
        // Navigation effects (open a channel, leave) are collected by the root, as with the Compose screen
        observations = [
            SwiftBridgeKt.observe(flow: component.state) { [weak self] value in
                if let state = value as? ChannelListState { self?.state = state }
            },
            SwiftBridgeKt.observe(flow: root.connectionStatus) { [weak self] value in
                if let status = value as? ConnectionStatus { self?.badge = ConnectionBadgeKt.badgeStatus(status: status) }
            },
            SwiftBridgeKt.observe(flow: root.isCacheEnabled) { [weak self] value in
                self?.isCacheEnabled = (value as? Bool) ?? false
            },
        ]
    }

    deinit {
        observations.forEach { $0.cancel() }
    }

    func send(_ intent: ChannelListIntent) {
        component.onIntent(intent: intent)
    }
}

/// Channel list in SwiftUI (32.4), over the same `ChannelListComponent` as the Compose one.
struct ChannelListView: View {
    let root: RootComponent
    @StateObject private var model: ChannelListModel
    @StateObject private var texts = Texts([
        "channels_title", "channels_subtitle", "channels_empty", "channels_disconnect", "channels_new_hint",
        "channels_join", "common_history", "common_settings", "stop_host_title", "stop_host_text", "stop_host_confirm",
        "common_cancel",
    ])
    @State private var serverCodeText = ""
    @Environment(\.pttTheme) private var theme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    private var fieldAndButton: AnyLayout { dockLayout(dynamicTypeSize) }

    init(component: ChannelListComponent, root: RootComponent) {
        self.root = root
        _model = StateObject(wrappedValue: ChannelListModel(component: component, root: root))
    }

    private var state: ChannelListState { model.state }

    var body: some View {
        Group {
            ScrollView {
                VStack(alignment: .leading, spacing: PttTheme.dimens.spaceLg) {
                    header
                    ForEach(state.activeChannels, id: \.id) { channel in
                        Button {
                            model.send(ChannelListIntentJoinChannel(channelId: channel.id, name: channel.id))
                        } label: {
                            ChannelCard(channel: channel)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("channel-\(channel.id)")
                    }
                }
                .padding(.horizontal, PttTheme.dimens.space2xl)
                .readableWidth()
            }
            .scrollDismissesKeyboard(.interactively)
            .background {
                ZStack {
                    theme.background
                    AmbientGlow(color: theme.statusOnline, intensity: topGlowIntensity, size: topGlowSize)
                        .offset(topGlowOffset)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                    AmbientGlow(color: theme.primary, intensity: bottomGlowIntensity, size: bottomGlowSize)
                        .offset(bottomGlowOffset)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                }
                .ignoresSafeArea()
            }
            .safeAreaInset(edge: .bottom) { newChannelDock }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(texts["channels_disconnect"], systemImage: "chevron.backward") {
                        model.send(ChannelListIntentLeave.shared)
                    }
                    .accessibilityIdentifier("leave")
                }
                ToolbarItem(placement: .principal) {
                    StatusBadge(status: model.badge)
                }
                ToolbarItemGroup(placement: .topBarTrailing) {
                    if model.isCacheEnabled {
                        Button(texts["common_history"], systemImage: "clock.arrow.circlepath") { root.navigateToHistory() }
                    }
                    Button(texts["common_settings"], systemImage: "slider.horizontal.3") { root.navigateToSettings() }
                }
            }
        }
        .alert(texts["stop_host_title"], isPresented: stopHostShown) {
            Button(texts["stop_host_confirm"], role: .destructive) { model.send(ChannelListIntentConfirmStopHost.shared) }
            Button(texts["common_cancel"], role: .cancel) { model.send(ChannelListIntentDismissStopHost.shared) }
        } message: {
            Text(texts["stop_host_text"])
        }
        .task(id: state.serverCode) {
            guard let code = state.serverCode else { return }
            serverCodeText = await Texts.format("channels_server_code", [code])
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: PttTheme.dimens.spaceXs) {
            Text(texts["channels_title"])
                .font(.largeTitle.bold())
                .foregroundStyle(theme.textPrimary)
            Text(state.activeChannels.isEmpty ? texts["channels_empty"] : texts["channels_subtitle"])
                .font(.body)
                .foregroundStyle(theme.textSecondary)
            // The host and everyone in its room see the same code; a different one means another server
            if state.serverCode != nil {
                Text(serverCodeText)
                    .font(.caption.monospaced())
                    .foregroundStyle(theme.textSecondary)
            }
        }
        .padding(.bottom, PttTheme.dimens.spaceXs)
    }

    /// New channel as a floating glass dock, like the Compose `NewChannelDock`.
    private var newChannelDock: some View {
        fieldAndButton {
            TextField(texts["channels_new_hint"], text: Binding(
                get: { state.newChannelName },
                set: { model.send(ChannelListIntentUpdateNewChannelName(name: $0)) }
            ))
            .foregroundStyle(theme.textPrimary)
            .padding(.horizontal, PttTheme.dimens.spaceXl)
            .frame(minHeight: PttTheme.dimens.touchTarget)
            .background(theme.background.opacity(fieldWellOpacity), in: Capsule())
            .submitLabel(.join)
            .onSubmit { model.send(ChannelListIntentCreateChannel.shared) }
            .accessibilityIdentifier("newChannel")
            Button(texts["channels_join"]) {
                model.send(ChannelListIntentCreateChannel.shared)
            }
            .controlSize(.large)
            .modifier(PrimaryGlassButtonStyle())
            .disabled(state.newChannelName.trimmingCharacters(in: .whitespaces).isEmpty)
            .accessibilityIdentifier("join")
        }
        .padding(PttTheme.dimens.spaceXl)
        .glassControl(in: RoundedRectangle(cornerRadius: PttTheme.dimens.dockRadius, style: .continuous))
        .padding(.horizontal, PttTheme.dimens.spaceLg)
        .padding(.bottom, PttTheme.dimens.spaceSm)
        .readableWidth()
    }

    private var stopHostShown: Binding<Bool> {
        Binding(get: { state.confirmingStopHost }, set: { if !$0 { model.send(ChannelListIntentDismissStopHost.shared) } })
    }
}

/// A channel as a content card: name, and how many are in it or "vazia" (`ChannelCard` in Compose).
private struct ChannelCard: View {
    let channel: ActiveChannelDomain
    @State private var name = ""
    @State private var people = ""
    @Environment(\.pttTheme) private var theme

    private var isEmpty: Bool { channel.participantCount == 0 }

    var body: some View {
        HStack(spacing: PttTheme.dimens.spaceLg) {
            VStack(alignment: .leading, spacing: PttTheme.dimens.spaceSm) {
                Text(name)
                    .font(.headline)
                    .foregroundStyle(isEmpty ? theme.textSecondary : theme.textPrimary)
                    .lineLimit(1)
                Label(people, systemImage: "person.2.fill")
                    .font(.caption)
                    .foregroundStyle(theme.textSecondary)
                    .labelStyle(TertiaryIconLabelStyle(iconColor: theme.textTertiary))
            }
            Spacer()
            Image(systemName: "chevron.right")
                .font(.footnote.weight(.semibold))
                .foregroundStyle(theme.textTertiary)
        }
        .padding(.horizontal, PttTheme.dimens.spaceXl)
        .padding(.vertical, PttTheme.dimens.spaceLg)
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .contentCard()
        .task(id: "\(channel.id)-\(channel.participantCount)") {
            name = await Texts.format("channel_name", [channel.id])
            people = isEmpty
                ? await Texts.format("channel_card_empty", [])
                : await Texts.plural("channel_card_people", Int(channel.participantCount), [channel.participantCount])
        }
    }
}

private struct TertiaryIconLabelStyle: LabelStyle {
    let iconColor: Color

    func makeBody(configuration: Configuration) -> some View {
        HStack(spacing: PttTheme.dimens.spaceSm) {
            configuration.icon.foregroundStyle(iconColor)
            configuration.title
        }
    }
}
