import XCTest

/// The SwiftUI connection screen over the Kotlin component (32.1): what the user types reaches the component as
/// intents, its state comes back into the fields, and its error effects show as an alert with the resource's text.
final class ConnectionUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        // Arguments override NSUserDefaults for this launch only: every test starts from empty fields, whatever the
        // app saved in an earlier run (keys from SettingsKeys)
        app.launchArguments = ["-nickname", "", "-manualIp", ""]
        app.launch()
    }

    func testAnInvalidAddressShowsTheComponentsErrorWithoutConnecting() {
        type("Teste UI", into: app.textFields["nickname"])
        type("ptt.example.com:99999", into: app.textFields["manualAddress"])
        app.buttons["connect"].tap()

        let alert = app.alerts.firstMatch
        XCTAssertTrue(alert.waitForExistence(timeout: 10))
        XCTAssertTrue(alert.label.hasPrefix("Endereço inválido"), alert.label)
        alert.buttons.firstMatch.tap()
        XCTAssertTrue(app.textFields["manualAddress"].exists, "an invalid address must not leave the screen")
    }

    /// Only where a server answers on the network (not on CI): connecting hands over to the channel list, still in
    /// Compose, through the shared navigation root.
    func testConnectingToAServerOnTheNetworkOpensTheChannelList() throws {
        type("Teste UI", into: app.textFields["nickname"])
        let server = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'PTT-LAN'")).firstMatch
        guard server.waitForExistence(timeout: 8) else {
            throw XCTSkip("no PTT-LAN server found on this network")
        }
        server.tap()

        XCTAssertTrue(app.textFields["nickname"].waitForNonExistence(timeout: 15), "the channel list should replace this screen")
    }

    private func type(_ text: String, into field: XCUIElement) {
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(text)
    }
}
