import SwiftUI
import shared

/// App root: the SwiftUI screen of the active child of the shared navigation root (32.4); no Compose on iOS (32.6).
struct RootView: View {
    let root: RootComponent
    @StateObject private var stack: ValueObserver<ChildStack<AnyObject, RootComponentChild>>
    @StateObject private var theme: ThemeModel

    init(root: RootComponent) {
        self.root = root
        _stack = StateObject(wrappedValue: ValueObserver(root.childStack))
        _theme = StateObject(wrappedValue: ThemeModel(root: root))
    }

    var body: some View {
        let active = stack.value.active.instance
        GeometryReader { geometry in
            PttThemed(model: theme) {
                HStack(spacing: 0) {
                    // iPad: the channel list, still alive under the open channel, stays beside it (27.2)
                    if let channels = channelsBeside(active, width: geometry.size.width) {
                        ChannelListView(component: channels.component, root: root)
                            .frame(width: PttTheme.dimens.channelPaneWidth)
                            .id(ObjectIdentifier(channels))
                    }
                    nativeScreen(active)
                        .id(ObjectIdentifier(active))
                }
            }
        }
    }

    /// The channel list under the open channel, when the window is wide enough to show both, like the Compose root.
    private func channelsBeside(_ active: RootComponentChild, width: CGFloat) -> RootComponentChildChannelListChild? {
        guard active is RootComponentChildPttChild, width >= PttTheme.dimens.expandedWidth else { return nil }
        return stack.value.backStack.last?.instance as? RootComponentChildChannelListChild
    }

    /// The SwiftUI screen for [child].
    @ViewBuilder
    private func nativeScreen(_ child: RootComponentChild) -> some View {
        switch child {
        case let connection as RootComponentChildConnectionChild:
            ConnectionView(component: connection.component, root: root)
        case let channels as RootComponentChildChannelListChild:
            ChannelListView(component: channels.component, root: root)
        case let ptt as RootComponentChildPttChild:
            PttView(component: ptt.component, root: root)
        case let settings as RootComponentChildSettingsChild:
            SettingsView(component: settings.component, root: root)
        case let history as RootComponentChildHistoryChild:
            HistoryView(component: history.component)
        default:
            EmptyView()
        }
    }
}
