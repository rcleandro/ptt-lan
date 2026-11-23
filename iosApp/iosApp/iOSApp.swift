import SwiftUI
import shared

@main
struct iOSApp: App {
    private let root: RootComponent

    init() {
        IosDependenciesKt.doInitKoinIos()
        root = SwiftBridgeKt.createRootComponent()
    }

    var body: some Scene {
        WindowGroup {
            RootView(root: root)
        }
    }
}
