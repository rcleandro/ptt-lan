import SwiftUI
import shared

@main
struct iOSApp: App {
    private let root: RootComponent
    private let watch: WatchSession

    init() {
        IosDependenciesKt.doInitKoinIos()
        root = SwiftBridgeKt.createRootComponent()
        watch = WatchSession(root: root)
    }

    var body: some Scene {
        WindowGroup {
            RootView(root: root)
        }
    }
}
