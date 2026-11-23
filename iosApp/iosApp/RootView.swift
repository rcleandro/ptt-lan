import SwiftUI
import UIKit
import shared

/// One step of the navigation path: a child of the Decompose stack, the same entry for as long as it stays there.
private struct Destination: Hashable {
    let child: RootComponentChild

    static func == (lhs: Destination, rhs: Destination) -> Bool { lhs.child === rhs.child }

    func hash(into hasher: inout Hasher) { hasher.combine(ObjectIdentifier(child)) }
}

/// App root: a `NavigationStack` whose path is the shared root's child stack (32.2), so screens push and pop with the
/// system animation and the edge swipe, while Kotlin keeps deciding the navigation. No Compose on iOS (32.6).
struct RootView: View {
    let root: RootComponent
    @StateObject private var stack: ValueObserver<ChildStack<AnyObject, RootComponentChild>>
    @StateObject private var theme: ThemeModel
    @State private var path: [Destination] = []
    @State private var width: CGFloat = 0

    init(root: RootComponent) {
        self.root = root
        _stack = StateObject(wrappedValue: ValueObserver(root.childStack))
        _theme = StateObject(wrappedValue: ThemeModel(root: root))
    }

    private var isWide: Bool { width >= PttTheme.dimens.expandedWidth }

    var body: some View {
        PttThemed(model: theme) {
            HStack(spacing: 0) {
                // iPad: the channel list, still alive under the open channel, stays beside it (27.2)
                if let channels = channelsBeside {
                    NavigationStack {
                        ChannelListView(component: channels.component, root: root)
                    }
                    .frame(width: PttTheme.dimens.channelPaneWidth)
                    .id(ObjectIdentifier(channels))
                }
                NavigationStack(path: pathBinding) {
                    screen(stack.value.items.first!.instance)
                        .navigationDestination(for: Destination.self) { destination in
                            // Each screen keeps its own back button: leaving the list is "Desconectar", not "<"
                            screen(destination.child).navigationBarBackButtonHidden()
                        }
                }
            }
        }
        .onGeometryChange(for: CGFloat.self) { $0.size.width } action: { width = $0 }
        .onReceive(stack.$value) { value in
            // Wide, switching channels beside the list would slide the channel out and in: no animation, like Compose
            var transaction = Transaction()
            transaction.disablesAnimations = isWide
            withTransaction(transaction) { path = destinations(value) }
        }
    }

    /// Everything above the first screen (the connection, always at the bottom of the stack).
    private func destinations(_ value: ChildStack<AnyObject, RootComponentChild>) -> [Destination] {
        value.items.dropFirst().map { Destination(child: $0.instance) }
    }

    /// Pops made by the system (the edge swipe) go through the shared root's back, as on Android; its new stack then
    /// comes back through `onReceive`.
    private var pathBinding: Binding<[Destination]> {
        Binding(
            get: { path },
            set: { newPath in
                let popped = path.count - newPath.count
                path = newPath
                for _ in 0 ..< max(popped, 0) {
                    _ = SwiftBridgeKt.goBack()
                }
            }
        )
    }

    /// The channel list under the open channel, when the window is wide enough to show both, like the Compose root.
    private var channelsBeside: RootComponentChildChannelListChild? {
        guard stack.value.active.instance is RootComponentChildPttChild, isWide else { return nil }
        return stack.value.backStack.last?.instance as? RootComponentChildChannelListChild
    }

    /// The SwiftUI screen for [child].
    @ViewBuilder
    private func screen(_ child: RootComponentChild) -> some View {
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

/// Hiding the system back button also turns the edge swipe off; this turns it back on for every pushed screen.
extension UINavigationController: @retroactive UIGestureRecognizerDelegate {
    override open func viewDidLoad() {
        super.viewDidLoad()
        interactivePopGestureRecognizer?.delegate = self
    }

    public func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
        viewControllers.count > 1
    }
}
