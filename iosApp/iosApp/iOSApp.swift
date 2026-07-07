import SwiftUI
import shared

@main
struct iOSApp: App {
    
    init() {
        IosDependenciesKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
