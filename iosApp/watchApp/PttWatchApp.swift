import SwiftUI

/// The watch app (33.1): a remote for the iPhone app, which keeps the connection, the microphone and the audio
/// (ADR 0013). Dark only, like the Wear OS app.
@main
struct PttWatchApp: App {
    var body: some Scene {
        WindowGroup {
            Image(systemName: "mic.fill")
                .font(.largeTitle)
                .foregroundStyle(WatchPalette.primary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(WatchPalette.background)
        }
    }
}
