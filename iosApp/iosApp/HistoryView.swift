import SwiftUI
import shared

// Ambient light, the same as the Compose screen: blue from the bottom corner
private let glowSize: CGFloat = 460
private let glowOffset = CGSize(width: -160, height: 120)
private let glowIntensity = 0.26
private let emptyIconSize: CGFloat = 64
private let messagePlayButtonSize: CGFloat = 40
private let playerControlSize: CGFloat = 52
private let progressLabelOpacity = 0.7

/// The history and its player, kept while the screen is shown. Every rule (groups, texts, the queue) is Kotlin.
final class HistoryModel: ObservableObject {
    let component: HistoryComponent
    @Published private(set) var messages: [VoiceMessage] = []
    @Published private(set) var playingMessageId: String?
    @Published private(set) var isPaused = false
    @Published private(set) var position: PlaybackPosition?
    @Published private(set) var speed: Float = 1
    @Published private(set) var queue: [VoiceMessage] = []
    private var observations: [Observation] = []

    init(component: HistoryComponent) {
        self.component = component
        let playback = component.playback
        observations = [
            SwiftBridgeKt.observe(flow: component.messages) { [weak self] in self?.messages = $0 as? [VoiceMessage] ?? [] },
            SwiftBridgeKt.observe(flow: playback.playingMessageId) { [weak self] in self?.playingMessageId = $0 as? String },
            SwiftBridgeKt.observe(flow: playback.isPaused) { [weak self] in self?.isPaused = ($0 as? Bool) ?? false },
            SwiftBridgeKt.observe(flow: playback.playbackPosition) { [weak self] in self?.position = $0 as? PlaybackPosition },
            SwiftBridgeKt.observe(flow: playback.playbackSpeed) { [weak self] in self?.speed = ($0 as? Float) ?? 1 },
            SwiftBridgeKt.observe(flow: playback.queue) { [weak self] in self?.queue = $0 as? [VoiceMessage] ?? [] },
        ]
    }

    deinit {
        observations.forEach { $0.cancel() }
    }

    var playback: HistoryPlayback { component.playback }
    var playingMessage: VoiceMessage? { messages.first { $0.id == playingMessageId } }
}

/// History in SwiftUI (32.4), over the same `HistoryComponent` and `HistoryPlayback` as the Compose screen.
struct HistoryView: View {
    @StateObject private var model: HistoryModel
    @StateObject private var texts = Texts([
        "history_title", "history_subtitle", "history_empty", "history_back", "history_clear", "history_clear_text",
        "history_clear_confirm", "history_delete_title", "history_delete_text", "history_delete_confirm",
        "history_delete_channel_title", "common_cancel",
    ])
    @State private var collapsed: Set<String> = []
    @State private var messageToDelete: VoiceMessage?
    @State private var channelToDelete: String?
    @State private var channelToDeleteText = ""
    @State private var confirmingClear = false
    @Environment(\.pttTheme) private var theme

    init(component: HistoryComponent) {
        _model = StateObject(wrappedValue: HistoryModel(component: component))
    }

    var body: some View {
        Group {
            content
                .background {
                    ZStack {
                        theme.background
                        AmbientGlow(color: theme.primary, intensity: glowIntensity, size: glowSize)
                            .offset(glowOffset)
                            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomLeading)
                    }
                    .ignoresSafeArea()
                }
                .safeAreaInset(edge: .bottom) {
                    if let message = model.playingMessage {
                        MiniPlayer(model: model, message: message)
                    }
                }
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        Button(texts["history_back"], systemImage: "chevron.backward") { model.component.onBack() }
                            .accessibilityIdentifier("historyBack")
                    }
                    if !model.messages.isEmpty {
                        ToolbarItem(placement: .topBarTrailing) {
                            Button(texts["history_clear"], systemImage: "trash") { confirmingClear = true }
                                .tint(theme.statusOffline)
                        }
                    }
                }
        }
        .alert(texts["history_delete_title"], isPresented: shown($messageToDelete)) {
            Button(texts["history_delete_confirm"], role: .destructive) {
                if let message = messageToDelete { model.component.deleteMessage(message: message) }
            }
            Button(texts["common_cancel"], role: .cancel) {}
        } message: {
            Text(texts["history_delete_text"])
        }
        .alert(texts["history_delete_channel_title"], isPresented: shown($channelToDelete)) {
            Button(texts["history_delete_confirm"], role: .destructive) {
                if let channel = channelToDelete { model.component.deleteChannelMessages(channelId: channel) }
            }
            Button(texts["common_cancel"], role: .cancel) {}
        } message: {
            Text(channelToDeleteText)
        }
        .alert(texts["history_clear"], isPresented: $confirmingClear) {
            Button(texts["history_clear_confirm"], role: .destructive) { model.component.clearAllMessages() }
            Button(texts["common_cancel"], role: .cancel) {}
        } message: {
            Text(texts["history_clear_text"])
        }
        .task(id: channelToDelete) {
            guard let channel = channelToDelete else { return }
            channelToDeleteText = await Texts.format("history_delete_channel_text", [channel])
        }
    }

    @ViewBuilder
    private var content: some View {
        if model.messages.isEmpty {
            VStack(spacing: PttTheme.dimens.spaceXl) {
                Image(systemName: "clock.arrow.circlepath")
                    .font(.system(size: emptyIconSize))
                    .foregroundStyle(theme.textTertiary)
                Text(texts["history_empty"])
                    .foregroundStyle(theme.textSecondary)
                    .multilineTextAlignment(.center)
            }
            .padding(PttTheme.dimens.space2xl)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else {
            ScrollView {
                VStack(alignment: .leading, spacing: PttTheme.dimens.spaceMd) {
                    VStack(alignment: .leading, spacing: PttTheme.dimens.spaceXs) {
                        Text(texts["history_title"]).font(.largeTitle.bold()).foregroundStyle(theme.textPrimary)
                        Text(texts["history_subtitle"]).foregroundStyle(theme.textSecondary)
                    }
                    .padding(.bottom, PttTheme.dimens.spaceMd)
                    ForEach(HistoryTextKt.historyGroups(messages: model.messages), id: \.channelId) { group in
                        let isCollapsed = collapsed.contains(group.channelId)
                        ChannelHeader(
                            group: group,
                            isCollapsed: isCollapsed,
                            onToggle: { toggle(group.channelId) },
                            onPlay: { model.playback.playChannel(channelId: group.channelId) },
                            onPlayUnheard: { model.playback.playUnheard(channelId: group.channelId) },
                            onDelete: { channelToDelete = group.channelId }
                        )
                        if !isCollapsed {
                            VStack(spacing: 0) {
                                ForEach(Array(group.messages.enumerated()), id: \.element.id) { index, message in
                                    if index > 0 { Divider().overlay(theme.border).padding(.leading, playerControlSize + PttTheme.dimens.spaceLg) }
                                    MessageRow(
                                        message: message,
                                        isPlaying: message.id == model.playingMessageId,
                                        isPaused: message.id == model.playingMessageId && model.isPaused,
                                        onPlay: { model.playback.playMessage(message: message) },
                                        onShare: { model.component.shareMessage(message: message) },
                                        onDelete: { messageToDelete = message }
                                    )
                                }
                            }
                            .contentCard()
                        }
                    }
                }
                .padding(.horizontal, PttTheme.dimens.space2xl)
                .padding(.bottom, PttTheme.dimens.space2xl)
                .readableWidth()
            }
        }
    }

    private func toggle(_ channelId: String) {
        withAnimation {
            if collapsed.contains(channelId) { collapsed.remove(channelId) } else { collapsed.insert(channelId) }
        }
    }

    private func shown<T>(_ item: Binding<T?>) -> Binding<Bool> {
        Binding(get: { item.wrappedValue != nil }, set: { if !$0 { item.wrappedValue = nil } })
    }
}

/// A room's header: collapse or expand, play its unheard messages or all of it, and delete it from the menu.
private struct ChannelHeader: View {
    let group: HistoryGroup
    let isCollapsed: Bool
    let onToggle: () -> Void
    let onPlay: () -> Void
    let onPlayUnheard: () -> Void
    let onDelete: () -> Void
    @StateObject private var texts = Texts(["history_play_room", "history_expand", "history_collapse", "history_delete_channel_title"])
    @State private var name = ""
    @State private var unheard = ""
    @Environment(\.pttTheme) private var theme

    var body: some View {
        HStack(spacing: PttTheme.dimens.spaceSm) {
            Button(action: onToggle) {
                HStack {
                    Text(name.uppercased()).font(.caption.weight(.semibold)).foregroundStyle(theme.textTertiary)
                    Spacer()
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            if group.unheardCount > 0 {
                Button(unheard, action: onPlayUnheard).font(.footnote.weight(.semibold)).modifier(GlassButtonStyle())
            }
            Button(texts["history_play_room"], systemImage: "text.line.first.and.arrowtriangle.forward", action: onPlay)
                .labelStyle(.iconOnly)
                .modifier(GlassButtonStyle())
            Button(isCollapsed ? texts["history_expand"] : texts["history_collapse"], systemImage: isCollapsed ? "chevron.down" : "chevron.up", action: onToggle)
                .labelStyle(.iconOnly)
                .foregroundStyle(theme.textSecondary)
                .buttonStyle(.plain)
        }
        .padding(.leading, PttTheme.dimens.spaceXl)
        .padding(.top, PttTheme.dimens.spaceMd)
        .contextMenu {
            Button(texts["history_delete_channel_title"], systemImage: "trash", role: .destructive, action: onDelete)
        }
        .task(id: "\(group.channelId)-\(group.unheardCount)") {
            name = await Texts.format("channel_name", [group.channelId])
            unheard = await Texts.plural("history_unheard", Int(group.unheardCount), [group.unheardCount])
        }
    }
}

/// A message: play or pause, who spoke, when or how far along, and share or delete.
private struct MessageRow: View {
    let message: VoiceMessage
    let isPlaying: Bool
    let isPaused: Bool
    let onPlay: () -> Void
    let onShare: () -> Void
    let onDelete: () -> Void
    @StateObject private var texts = Texts(["history_share", "history_delete"])
    @State private var status = ""
    @Environment(\.pttTheme) private var theme

    var body: some View {
        HStack(spacing: PttTheme.dimens.spaceLg) {
            PlayButton(isPlaying: isPlaying && !isPaused, isActive: isPlaying, size: messagePlayButtonSize, action: onPlay)
            VStack(alignment: .leading, spacing: PttTheme.dimens.spaceXs) {
                Text(message.senderNickname).font(.headline).foregroundStyle(theme.textPrimary).lineLimit(1)
                Text(status).font(.caption).foregroundStyle(isPlaying ? theme.primary : theme.textSecondary)
            }
            Spacer()
            Button(texts["history_share"], systemImage: "square.and.arrow.up", action: onShare)
                .labelStyle(.iconOnly).foregroundStyle(theme.textTertiary).buttonStyle(.plain)
            Button(texts["history_delete"], systemImage: "trash", action: onDelete)
                .labelStyle(.iconOnly).foregroundStyle(theme.textTertiary).buttonStyle(.plain)
        }
        .padding(.horizontal, PttTheme.dimens.spaceLg)
        .padding(.vertical, PttTheme.dimens.spaceMd)
        .task(id: "\(message.id)-\(isPlaying)-\(isPaused)") {
            let text = SwiftBridgeKt.historyMessageStatus(message: message, isPlaying: isPlaying, isPaused: isPaused)
            status = (try? await SwiftBridgeKt.text(uiText: text)) ?? ""
        }
    }
}

/// Round play or pause button, filled with the action color while its message is the one playing.
private struct PlayButton: View {
    let isPlaying: Bool
    let isActive: Bool
    let size: CGFloat
    let action: () -> Void
    @StateObject private var texts = Texts(["history_play", "history_pause"])
    @Environment(\.pttTheme) private var theme

    var body: some View {
        // The circle is the label, so the whole of it takes the tap, not only the icon
        Button(action: action) {
            Image(systemName: isPlaying ? "pause.fill" : "play.fill")
                .foregroundStyle(isActive ? theme.onPrimary : theme.textPrimary)
                .frame(width: size, height: size)
                .background(isActive ? theme.primary : theme.surface, in: Circle())
                .overlay(Circle().stroke(theme.border, lineWidth: 1))
                .contentShape(Circle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isPlaying ? texts["history_pause"] : texts["history_play"])
    }
}

/// The message playing, as a floating Liquid Glass dock: sender, room and queue, seek bar and transport.
private struct MiniPlayer: View {
    @ObservedObject var model: HistoryModel
    let message: VoiceMessage
    @StateObject private var texts = Texts(["history_previous", "history_next", "history_back_5", "history_forward_5"])
    @State private var subtitle = ""
    @State private var speedLabel = ""
    @State private var dragFraction: Double?
    @Environment(\.pttTheme) private var theme

    private var queueIndex: Int { model.queue.firstIndex { $0.id == message.id } ?? -1 }
    private var position: PlaybackPosition? { model.position?.messageId == message.id ? model.position : nil }
    private var totalMs: Int64 { position?.durationMs ?? message.durationMs }
    private var shownMs: Int64 {
        if let dragFraction { return Int64(dragFraction * Double(totalMs)) }
        return position?.positionMs ?? 0
    }

    var body: some View {
        VStack(spacing: PttTheme.dimens.spaceSm) {
            HStack(spacing: PttTheme.dimens.spaceLg) {
                Text(message.senderNickname.prefix(1).uppercased())
                    .font(.headline)
                    .foregroundStyle(theme.textPrimary)
                    .frame(width: playerControlSize, height: playerControlSize)
                    .background(theme.primaryGlow, in: Circle())
                VStack(alignment: .leading, spacing: PttTheme.dimens.spaceXs) {
                    Text(message.senderNickname).font(.headline).foregroundStyle(theme.textPrimary).lineLimit(1)
                    Text(subtitle.uppercased()).font(.caption2.weight(.semibold)).foregroundStyle(theme.textTertiary)
                }
                Spacer()
                PlayButton(isPlaying: !model.isPaused, isActive: true, size: playerControlSize) {
                    model.playback.playMessage(message: message)
                }
            }
            Slider(
                value: Binding(get: { dragFraction ?? Double(position?.fraction ?? 0) }, set: { dragFraction = $0 }),
                onEditingChanged: { editing in
                    guard !editing, let fraction = dragFraction else { return }
                    model.playback.seekTo(positionMs: Int64(fraction * Double(totalMs)))
                    dragFraction = nil
                }
            )
            .accessibilityIdentifier("seek")
            HStack {
                Text(HistoryTextKt.positionText(positionMs: shownMs))
                Spacer()
                Text(HistoryTextKt.durationText(durationMs: totalMs))
            }
            .font(.caption.monospacedDigit())
            .foregroundStyle(theme.textPrimary.opacity(progressLabelOpacity))
            transport
        }
        .padding(PttTheme.dimens.spaceLg)
        .glassControl(in: RoundedRectangle(cornerRadius: PttTheme.dimens.dockRadius, style: .continuous))
        .padding(.horizontal, PttTheme.dimens.spaceLg)
        .padding(.bottom, PttTheme.dimens.spaceSm)
        .readableWidth()
        .task(id: "\(message.id)-\(model.isPaused)-\(queueIndex)-\(model.queue.count)") {
            let text = HistoryTextKt.playerSubtitle(message: message, isPaused: model.isPaused, queueIndex: Int32(queueIndex), queueSize: Int32(model.queue.count))
            subtitle = (try? await SwiftBridgeKt.text(uiText: text)) ?? ""
        }
        .task(id: model.speed) {
            speedLabel = (try? await SwiftBridgeKt.text(uiText: HistoryTextKt.speedText(speed: model.speed))) ?? ""
        }
    }

    /// Previous, back 5 s, the speed, forward 5 s and next; the skips only while a room plays in sequence.
    private var transport: some View {
        HStack {
            if queueIndex >= 0 {
                Button(texts["history_previous"], systemImage: "backward.end.fill") { model.playback.playPrevious() }
            }
            Spacer()
            Button(texts["history_back_5"], systemImage: "gobackward.5") {
                model.playback.seekTo(positionMs: max(0, shownMs - HistoryTextKt.SEEK_STEP_MS))
            }
            Spacer()
            Button(speedLabel) { model.playback.cycleSpeed() }
                .font(.callout.weight(.semibold))
                .accessibilityIdentifier("speed")
            Spacer()
            Button(texts["history_forward_5"], systemImage: "goforward.5") {
                model.playback.seekTo(positionMs: shownMs + HistoryTextKt.SEEK_STEP_MS)
            }
            if queueIndex >= 0 {
                Spacer()
                Button(texts["history_next"], systemImage: "forward.end.fill") { model.playback.playNext() }
                    .disabled(queueIndex >= model.queue.count - 1)
            }
        }
        .labelStyle(.iconOnly)
        .foregroundStyle(theme.textPrimary)
        .buttonStyle(.plain)
        .padding(.horizontal, PttTheme.dimens.spaceSm)
    }
}
