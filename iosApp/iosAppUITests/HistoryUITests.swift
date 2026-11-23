import XCTest

/// The SwiftUI history over the Kotlin component and player (32.4). Each test turns saving audio on through the
/// settings and leaves it as it found it.
final class HistoryUITests: XCTestCase {
    private let app = XCUIApplication()
    private var savingWasOn = false

    override func setUp() {
        continueAfterFailure = false
        app.launchArguments = ["-nickname", "", "-manualIp", ""]
        app.launch()
        savingWasOn = setSavingAudio(on: true)
    }

    override func tearDown() {
        if !savingWasOn, app.state == .runningForeground {
            app.launch()
            _ = setSavingAudio(on: false)
        }
    }

    func testAnEmptyOrFilledHistoryOpensFromTheConnectionScreenAndGoesBack() {
        let history = app.buttons["Histórico"]
        XCTAssertTrue(history.waitForExistence(timeout: 10), "saving audio should show the history button")
        history.tap()
        XCTAssertTrue(app.buttons["historyBack"].waitForExistence(timeout: 10))
        app.buttons["historyBack"].tap()
        XCTAssertTrue(app.textFields["nickname"].waitForExistence(timeout: 10))
    }

    /// Only where a server answers on the network (not on CI), with the microphone allowed: the user's own speech
    /// is recorded and shows in the history, and playing it does not crash.
    func testWhatTheUserSaidShowsInTheHistoryAndPlays() throws {
        let nickname = app.textFields["nickname"]
        XCTAssertTrue(nickname.waitForExistence(timeout: 10))
        nickname.tap()
        nickname.typeText("Teste UI")
        let server = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'PTT-LAN'")).firstMatch
        guard server.waitForExistence(timeout: 8) else {
            throw XCTSkip("no PTT-LAN server found on this network")
        }
        server.tap()
        let channel = app.buttons["channel-Geral"]
        XCTAssertTrue(channel.waitForExistence(timeout: 15))
        channel.tap()
        let lens = app.buttons["ptt"]
        XCTAssertTrue(lens.waitForExistence(timeout: 10))
        lens.press(forDuration: 2)

        app.buttons["Histórico"].tap()
        let play = app.buttons["Ouvir"].firstMatch
        XCTAssertTrue(play.waitForExistence(timeout: 10), "the message just spoken should be in the history")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "history"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        // The simulator records next to no audio (under a second for a 2 s hold), so the replay ends before the mini
        // player can be checked: this only proves that playing neither crashes nor leaves the list
        play.tap()
        XCTAssertTrue(app.buttons["historyBack"].waitForExistence(timeout: 5))

        app.buttons["historyBack"].tap()
        app.buttons["backToChannels"].tap()
        app.buttons["leave"].tap()
    }

    /// Turns "Salvar áudios recebidos" to [on] in the settings and comes back; returns whether it was on.
    private func setSavingAudio(on: Bool) -> Bool {
        let settings = app.buttons["Configurações"]
        XCTAssertTrue(settings.waitForExistence(timeout: 10))
        settings.tap()
        let toggle = app.switches["saveAudio"]
        XCTAssertTrue(toggle.waitForExistence(timeout: 10))
        let wasOn = toggle.value as? String == "1"
        if wasOn != on { toggle.switches.firstMatch.tap() }
        app.buttons["settingsBack"].tap()
        return wasOn
    }
}
