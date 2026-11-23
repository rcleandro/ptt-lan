import SwiftUI
import shared

/// The Compose screens not migrated yet, over the same navigation root as the SwiftUI ones.
private struct ComposeRootView: UIViewControllerRepresentable {
    let root: RootComponent

    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController(root: root)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// App root during the migration (32.4): Compose stays underneath, kept alive across screens, and the screens drawn in
/// SwiftUI cover it while they are the active one. Compose leaves those screens empty (`isNativeScreen`).
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
        ZStack {
            ComposeRootView(root: root).ignoresSafeArea()
            if MainViewControllerKt.isNativeScreen(child: active),
               let connection = active as? RootComponentChildConnectionChild {
                PttThemed(model: theme) {
                    ConnectionView(component: connection.component, root: root)
                }
                .id(ObjectIdentifier(connection.component))
            }
        }
    }
}
