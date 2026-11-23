import Foundation
import shared
import WatchConnectivity

/// The iPhone side of the watch remote (33.2): sends the watch what Kotlin decides it shows, and hands the watch's
/// commands back to Kotlin, which runs them through the same intents as the iPhone's screens.
final class WatchSession: NSObject, WCSessionDelegate {
    private let root: RootComponent
    private var observation: Observation?

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
                self?.send(message)
            }
        }
    }

    /// Only while the watch app is open: when it opens, or the iPhone comes back in range, it asks for the state
    /// itself (`sync`), so nothing saved and possibly stale is ever shown there.
    private func send(_ message: [String: Any]) {
        let session = WCSession.default
        guard session.activationState == .activated, session.isReachable else { return }
        session.sendMessage(message, replyHandler: nil, errorHandler: nil)
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        DispatchQueue.main.async { _ = self.root.onWatchMessage(message: message) }
    }

    /// Only the watch's `sync` waits for a reply: the iPhone's state right now.
    func session(_ session: WCSession, didReceiveMessage message: [String: Any], replyHandler: @escaping ([String: Any]) -> Void) {
        DispatchQueue.main.async { replyHandler(self.root.onWatchMessage(message: message)) }
    }

    func sessionDidBecomeInactive(_ session: WCSession) {}

    /// The user switched to another watch: the session has to be activated again for it.
    func sessionDidDeactivate(_ session: WCSession) {
        WCSession.default.activate()
    }
}
