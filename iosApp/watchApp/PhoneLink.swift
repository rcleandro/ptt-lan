import Foundation
import WatchConnectivity

/// The watch side of the remote (33.2): the iPhone's latest state, whether the iPhone can be reached, and the
/// commands the screens send it.
final class PhoneLink: NSObject, ObservableObject, WCSessionDelegate {
    @Published private(set) var state = PhoneState()
    @Published private(set) var isReachable = false

    override init() {
        super.init()
        guard WCSession.isSupported() else { return }
        WCSession.default.delegate = self
        WCSession.default.activate()
    }

    func press() { send([WatchKeys.command: WatchKeys.press]) }

    func release() { send([WatchKeys.command: WatchKeys.release]) }

    func leave() { send([WatchKeys.command: WatchKeys.leave]) }

    func join(_ channel: String) { send([WatchKeys.command: WatchKeys.join, WatchKeys.channel: channel]) }

    /// Sent only while the iPhone answers: a command that arrives late (a press after the release) does more harm
    /// than one that never arrives, and the screens disable themselves meanwhile (33.4).
    private func send(_ command: [String: Any]) {
        guard WCSession.default.isReachable else { return }
        WCSession.default.sendMessage(command, replyHandler: nil, errorHandler: nil)
    }

    func session(_ session: WCSession, activationDidCompleteWith state: WCSessionActivationState, error: Error?) {
        let context = session.receivedApplicationContext
        let reachable = session.isReachable
        DispatchQueue.main.async {
            if !context.isEmpty { self.state = PhoneState(message: context) }
            self.isReachable = reachable
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        DispatchQueue.main.async { self.state = PhoneState(message: applicationContext) }
    }

    func session(_ session: WCSession, didReceiveMessage message: [String: Any]) {
        DispatchQueue.main.async { self.state = PhoneState(message: message) }
    }

    func sessionReachabilityDidChange(_ session: WCSession) {
        let reachable = session.isReachable
        DispatchQueue.main.async { self.isReachable = reachable }
    }
}
