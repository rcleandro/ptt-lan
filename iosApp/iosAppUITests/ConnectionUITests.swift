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

    /// With a server (`PTT_SERVER_ADDRESS` on CI, else one found on the network): connecting opens the channel list with the server's
    /// default channel, and leaving goes back to this screen, all through the shared navigation root.
    func testConnectingListsTheChannelsAndLeavingComesBack() throws {
        type("Teste UI", into: app.textFields["nickname"])
        try connectToServer(app)

        XCTAssertTrue(app.buttons["channel-Geral"].waitForExistence(timeout: 15), "the channel list should show the default channel")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "channel-list"
        screenshot.lifetime = .keepAlways
        add(screenshot)

        app.buttons["leave"].tap()
        XCTAssertTrue(app.textFields["nickname"].waitForExistence(timeout: 10), "leaving should come back to the connection screen")
    }

    /// With a server: the edge swipe on the channel list goes through the list's own back (32.2), which disconnects
    /// like "Desconectar" instead of just uncovering the connection screen.
    func testSwipingBackFromTheChannelListDisconnects() throws {
        type("Teste UI", into: app.textFields["nickname"])
        try connectToServer(app)
        XCTAssertTrue(app.buttons["channel-Geral"].waitForExistence(timeout: 15))

        let edge = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0.5))
        edge.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)))

        XCTAssertTrue(app.textFields["nickname"].waitForExistence(timeout: 10), "the swipe should come back to the connection screen")
        XCTAssertTrue(app.buttons["connect"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["channel-Geral"].exists, "the channel list should be gone, not just covered")
    }

    /// With a server (`PTT_SERVER_ADDRESS` on CI, else one found on the network), with the microphone already allowed: the SwiftUI PTT
    /// screen opens from the list, holding the lens talks, and back returns to the list.
    func testTheChannelOpensThePttScreenAndHoldingTheLensTalks() throws {
        type("Teste UI", into: app.textFields["nickname"])
        try connectToServer(app)
        let channel = app.buttons["channel-Geral"]
        XCTAssertTrue(channel.waitForExistence(timeout: 15))
        channel.tap()

        let lens = app.buttons["ptt"]
        XCTAssertTrue(lens.waitForExistence(timeout: 10), "the channel should open the PTT screen")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "ptt"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        lens.press(forDuration: 1)

        app.buttons["backToChannels"].tap()
        XCTAssertTrue(app.buttons["channel-Geral"].waitForExistence(timeout: 10), "back should return to the channel list")
        app.buttons["leave"].tap()
    }

    /// Only on a wide screen (iPad), with a server: the channel list stays beside the open channel (27.2), and
    /// back leaves the list alone.
    func testOnAWideScreenTheChannelListStaysBesideTheChannel() throws {
        let wideScreen: CGFloat = 840 // ExpandedWidth in the design system
        guard app.windows.firstMatch.frame.width >= wideScreen else {
            throw XCTSkip("the screen is too narrow for two panes")
        }
        type("Teste UI", into: app.textFields["nickname"])
        try connectToServer(app)
        let channel = app.buttons["channel-Geral"]
        XCTAssertTrue(channel.waitForExistence(timeout: 15))
        channel.tap()

        XCTAssertTrue(app.buttons["ptt"].waitForExistence(timeout: 10), "the channel should open the PTT screen")
        XCTAssertTrue(channel.isHittable, "the channel list should stay beside the channel")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "side-by-side"
        screenshot.lifetime = .keepAlways
        add(screenshot)

        app.buttons["backToChannels"].tap()
        XCTAssertTrue(app.buttons["ptt"].waitForNonExistence(timeout: 10), "back should close the channel")
        app.buttons["leave"].tap()
    }

    private func type(_ text: String, into field: XCUIElement) {
        XCTAssertTrue(field.waitForExistence(timeout: 10))
        field.tap()
        field.typeText(text)
    }
}
