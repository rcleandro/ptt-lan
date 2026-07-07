import SwiftUI
import shared

@main
struct iOSApp: App {
    
    init() {
        IosDependenciesKt.initKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
