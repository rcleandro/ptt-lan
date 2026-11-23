/// Keys and values of the messages with the iPhone, the same as Kotlin's `WatchKeys` (`WatchKeysTest` checks both).
enum WatchKeys {
    static let connected = "connected"
    static let channels = "channels"
    static let id = "id"
    static let people = "people"
    static let channel = "channel"
    static let button = "button"
    static let speaker = "speaker"
    static let busy = "busy"
    static let command = "command"
    static let press = "press"
    static let release = "release"
    static let join = "join"
    static let leave = "leave"
}

/// The PTT button's state, named like Kotlin's `PttButtonState`, which decides it on the iPhone.
enum ButtonState: String {
    case idle = "Idle"
    case requesting = "Requesting"
    case transmitting = "Transmitting"
    case receiving = "Receiving"
}

/// A channel in the list: its name and how many people are in it.
struct WatchChannel: Equatable, Identifiable {
    let id: String
    let people: Int
}

/// What the iPhone says the watch shows; the watch only draws it.
struct PhoneState: Equatable {
    var connected = false
    var channels: [WatchChannel] = []
    var channel: String?
    var people = 0
    var button: ButtonState?
    var speaker: String?
    var busy = false

    init() {}

    init(message: [String: Any]) {
        connected = message[WatchKeys.connected] as? Bool ?? false
        channels = (message[WatchKeys.channels] as? [[String: Any]] ?? []).compactMap { entry in
            guard let id = entry[WatchKeys.id] as? String else { return nil }
            return WatchChannel(id: id, people: entry[WatchKeys.people] as? Int ?? 0)
        }
        channel = message[WatchKeys.channel] as? String
        people = message[WatchKeys.people] as? Int ?? 0
        button = (message[WatchKeys.button] as? String).flatMap(ButtonState.init(rawValue:))
        speaker = message[WatchKeys.speaker] as? String
        busy = message[WatchKeys.busy] as? Bool ?? false
    }
}
