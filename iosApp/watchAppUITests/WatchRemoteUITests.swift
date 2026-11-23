import XCTest

/// The watch as a remote for the iPhone (33.3): it lists the iPhone's channels, opens one through the iPhone, and
/// talks and leaves through it. Needs the paired iPhone's app connected to a server within a minute of the watch app
/// opening, so it skips anywhere else (CI). Start this first: installing the watch app closes the iPhone app.
final class WatchRemoteUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launch()
    }

    func testTheWatchOpensAChannelTalksAndLeavesThroughTheIphone() throws {
        let channel = app.buttons["channel-Geral"]
        guard channel.waitForExistence(timeout: 60) else {
            throw XCTSkip("the paired iPhone is not connected to a server")
        }
        channel.tap()

        let ptt = app.descendants(matching: .any)["ptt"]
        XCTAssertTrue(ptt.waitForExistence(timeout: 15), "the iPhone should open the channel and the watch follow it")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "watch-ptt"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        ptt.press(forDuration: 2)

        app.buttons["leave"].tap()
        XCTAssertTrue(channel.waitForExistence(timeout: 15), "leaving should bring back the channel list")
    }
}
