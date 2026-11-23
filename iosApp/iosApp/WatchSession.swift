import Foundation
import shared
import WatchConnectivity

/// The iPhone side of the watch remote (33.2): sends the watch what Kotlin decides it shows, and hands the watch's
/// commands back to Kotlin, which runs them through the same intents as the iPhone's screens.
final class WatchSession: NSObject, WCSessionDelegate {
    private let root: RootComponent
    private var observation: Observation?
    private var lastMessage: [String: Any] = [:]

    init(root: RootComponent) {
        self.root = root
        super.init()
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    deinit {
        observation?.cancel()
    }

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        guard state == .activated else { return }
        DispatchQueue.main.async {
            guard self.observation == nil else { return }
            self.observation = SwiftBridgeKt.observe(flow: self.root.watchMessages()) { [weak self] value in
                guard let message = value as? [String: Any] else { return }
                self?.lastMessage = message
                self?.send(message)
            }
        }
    }

    /// The application context keeps the latest state for when the watch app opens; a message reaches it right away
    /// while it is open.
    private func send(_ message: [String: Any]) {
        let session = WCSession.default
        guard session.activationState == .activated, session.isPaired, session.isWatchAppInstalled else { return }
        try? session.updateApplicationContext(message)
        if session.isReachable {
            session.sendMessage(message, replyHandler: nil, errorHandler: nil)
        }
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        DispatchQueue.main.async { self.root.onWatchMessage(message: message) }
    }

    func sessionReachabilityDidChange(_ session: WCSession) {
        guard session.isReachable else { return }
        DispatchQueue.main.async { self.send(self.lastMessage) }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    /// The user switched to another watch: the session has to be activated again for it.
    func sessionDidDeactivate(_ session: WCSession) {
        WCSession.default.activate()
    }
}
