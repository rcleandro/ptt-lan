import SwiftUI
import WatchKit

/// A text of the watch's string catalog, which repeats `strings.xml` under the same keys (`WatchStringsTest`).
func text(_ key: String, _ args: CVarArg...) -> String {
    String(format: NSLocalizedString(key, comment: ""), locale: .current, arguments: args)
}

/// The watch's screens (33.3), drawn from what the iPhone sends: the channels, then the PTT button of the open one.
/// Leaving a channel (the back chevron or "Sair") goes to the iPhone, which closes it there too.
struct WatchRootView: View {
    @StateObject private var link = PhoneLink()
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        NavigationStack {
            Group {
                if link.state.connected {
                    ChannelsView(link: link)
                } else if !link.isReachable {
                    MessageView(systemImage: "iphone.slash", message: text("watch_phone_unreachable"))
                } else {
                    MessageView(systemImage: "iphone", message: text("watch_connect_on_iphone"))
                }
            }
            .navigationDestination(isPresented: channelOpen) {
                PttView(link: link)
            }
        }
        .tint(WatchPalette.primary)
        .onChange(of: scenePhase) { phase in
            if phase == .active { link.sync() }
        }
    }

    /// Open while the iPhone is in a channel; the user going back asks the iPhone to leave it.
    private var channelOpen: Binding<Bool> {
        Binding(get: { link.state.channel != nil }, set: { if !$0 { link.leave() } })
    }
}

/// An icon over a short message, for the states with nothing to tap.
struct MessageView: View {
    let systemImage: String
    let message: String

    var body: some View {
        VStack(spacing: WatchDimens.spaceMd) {
            Image(systemName: systemImage)
                .font(.title2)
                .foregroundStyle(WatchPalette.textSecondary)
            Text(message)
                .font(.footnote)
                .multilineTextAlignment(.center)
                .foregroundStyle(WatchPalette.textPrimary)
        }
        .padding(WatchDimens.spaceMd)
    }
}

private struct ChannelsView: View {
    @ObservedObject var link: PhoneLink

    var body: some View {
        List {
            // Out of reach, the last channels stay in sight but cannot be opened (33.4)
            if !link.isReachable {
                Label(text("watch_phone_unreachable"), systemImage: "iphone.slash")
                    .font(.footnote)
                    .foregroundStyle(WatchPalette.statusOffline)
            }
            if link.state.channels.isEmpty {
                Text(text("watch_no_channels")).foregroundStyle(WatchPalette.textSecondary)
            }
            ForEach(link.state.channels) { channel in
                Button {
                    link.join(channel.id)
                } label: {
                    VStack(alignment: .leading) {
                        Text("# \(channel.id)").foregroundStyle(WatchPalette.textPrimary)
                        Text(String.localizedStringWithFormat(NSLocalizedString("channel_card_people", comment: ""), channel.people))
                            .font(.footnote)
                            .foregroundStyle(WatchPalette.textSecondary)
                    }
                }
                .accessibilityIdentifier("channel-\(channel.id)")
                .disabled(!link.isReachable)
            }
        }
        .navigationTitle(text("channels_title"))
    }
}

private struct PttView: View {
    @ObservedObject var link: PhoneLink
    @State private var isPressed = false

    private var state: PhoneState { link.state }
    private var button: ButtonState { state.button ?? .idle }

    var body: some View {
        VStack(spacing: WatchDimens.spaceSm) {
            PttButton(state: button, isPressed: isPressed)
                .opacity(link.isReachable ? 1 : WatchDimens.disabledOpacity)
                .gesture(holdToTalk, including: link.isReachable ? .all : .none)
                .accessibilityLabel(text(accessibilityKey))
                .accessibilityIdentifier("ptt")
            Text(status)
                .font(.footnote)
                .lineLimit(1)
                .foregroundStyle(isWarning ? WatchPalette.statusOffline : WatchPalette.textSecondary)
            Button(text("wear_leave")) { link.leave() }
                .controlSize(.mini)
                .disabled(!link.isReachable)
                .accessibilityIdentifier("leave")
        }
        // Channel and head count share the title, as the Wear OS app puts them next to the time
        .navigationTitle(state.channel.map { "# \($0) · \(state.people)" } ?? "")
        .navigationBarBackButtonHidden(isPressed || !link.isReachable)
        .onChange(of: link.isReachable) { reachable in
            // The iPhone lets go of the button itself when the watch drops out mid-press (WatchRemote.onUnreachable)
            if !reachable, isPressed {
                isPressed = false
                WKInterfaceDevice.current().play(.failure)
            }
        }
    }

    private var isWarning: Bool { !link.isReachable || (state.busy && button == .idle) }

    /// Press on touch, release on lift, with a tap on the wrist for each, like the Wear OS button.
    private var holdToTalk: some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { _ in
                guard !isPressed else { return }
                isPressed = true
                WKInterfaceDevice.current().play(.start)
                link.press()
            }
            .onEnded { _ in
                isPressed = false
                link.release()
                WKInterfaceDevice.current().play(.stop)
            }
    }

    private var status: String {
        if !link.isReachable { return text("watch_phone_unreachable") }
        return switch button {
        case .transmitting: text("wear_talking")
        case .requesting: text("wear_requesting")
        case .receiving: text("wear_receiving", state.speaker ?? text("ptt_someone"))
        case .idle: state.busy ? text("ptt_channel_busy") : text("ptt_status_free_headline")
        }
    }

    private var accessibilityKey: String {
        switch button {
        case .idle: "ptt_button_idle"
        case .requesting: "ptt_button_requesting"
        case .transmitting: "ptt_button_transmitting"
        case .receiving: "ptt_button_receiving"
        }
    }
}

/// The round button: quiet when the channel is free, amber for the user's own turn (outlined while asking for it),
/// blue while someone else talks, like the iPhone's lens.
private struct PttButton: View {
    let state: ButtonState
    let isPressed: Bool

    private let size: CGFloat = 112
    private let ring: CGFloat = 4
    private let pressedScale: CGFloat = 0.94

    var body: some View {
        ZStack {
            Circle().fill(fill)
            if state == .requesting {
                Circle().strokeBorder(WatchPalette.accentTx, lineWidth: ring)
            }
            Image(systemName: state == .receiving ? "speaker.wave.2.fill" : "mic.fill")
                .font(.title)
                .foregroundStyle(icon)
        }
        .frame(width: size, height: size)
        .scaleEffect(isPressed ? pressedScale : 1)
        .animation(.easeOut(duration: WatchDimens.pressAnimation), value: isPressed)
    }

    private var fill: Color {
        switch state {
        case .idle, .requesting: WatchPalette.surface
        case .transmitting: WatchPalette.accentTx
        case .receiving: WatchPalette.primary
        }
    }

    private var icon: Color {
        switch state {
        case .idle: WatchPalette.textPrimary
        case .requesting: WatchPalette.accentTx
        case .transmitting: WatchPalette.iconOnAccentTx
        case .receiving: WatchPalette.iconOnPrimary
        }
    }
}
