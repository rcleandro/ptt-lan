import SwiftUI
import shared

/// Texts of a screen, read from the same `strings.xml` as the Compose screens (32.1): no `Localizable` copy.
final class Texts: ObservableObject {
    @Published private var values: [String: String] = [:]

    init(_ keys: [String]) {
        Task { @MainActor in
            for key in keys {
                values[key] = try? await SwiftBridgeKt.text(key: key, args: [])
            }
        }
    }

    subscript(_ key: String) -> String {
        values[key] ?? ""
    }

    /// A text with placeholders (`%1$s`, `%2$d`), filled by the Kotlin side like on the other platforms.
    static func format(_ key: String, _ args: [Any]) async -> String {
        (try? await SwiftBridgeKt.text(key: key, args: args)) ?? ""
    }

    /// A plural (`<plurals>`), chosen for [quantity] by the Kotlin side, e.g. "3 pessoas".
    static func plural(_ key: String, _ quantity: Int, _ args: [Any]) async -> String {
        (try? await SwiftBridgeKt.pluralText(key: key, quantity: Int32(quantity), args: args)) ?? ""
    }
}

/// A Decompose `Value` as SwiftUI state, e.g. the navigation stack.
final class ValueObserver<T: AnyObject>: ObservableObject {
    @Published private(set) var value: T
    private var cancellation: Cancellation?

    init(_ source: Value<T>) {
        value = source.value
        cancellation = source.subscribe { [weak self] newValue in self?.value = newValue }
    }

    deinit {
        cancellation?.cancel()
    }
}

/// Text of a resource the component hands over (a hint, a label), loaded from the Kotlin side.
struct ResourceText: View {
    let resource: LibraryStringResource
    @State private var text = ""

    var body: some View {
        Text(text).task(id: ObjectIdentifier(resource)) {
            text = (try? await SwiftBridgeKt.text(resource: resource, args: [])) ?? ""
        }
    }
}
