import XCTest

/// The SwiftUI settings over the Kotlin component (32.4): every change goes to the component and comes back as
/// state. Each test leaves the settings as it found them.
final class SettingsUITests: XCTestCase {
    private let app = XCUIApplication()

    override func setUp() {
        continueAfterFailure = false
        app.launch()
        let settings = app.buttons["Configurações"]
        XCTAssertTrue(settings.waitForExistence(timeout: 10))
        settings.tap()
    }

    func testSavingAudioShowsTheHistoryOptionsAndTurningItOffHidesThem() {
        let saveAudio = app.switches["saveAudio"]
        XCTAssertTrue(saveAudio.waitForExistence(timeout: 10))
        let wasOn = saveAudio.value as? String == "1"
        if !wasOn { saveAudio.switches.firstMatch.tap() }

        XCTAssertTrue(app.sliders["cacheLimit"].waitForExistence(timeout: 5), "saving audio should show the space limit")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "settings"
        screenshot.lifetime = .keepAlways
        add(screenshot)

        saveAudio.switches.firstMatch.tap()
        XCTAssertTrue(app.sliders["cacheLimit"].waitForNonExistence(timeout: 5), "turning it off should hide the space limit")
        if wasOn { saveAudio.switches.firstMatch.tap() }
    }

    func testTheChosenThemeStaysSelectedAndBackLeavesTheScreen() {
        let dark = app.segmentedControls["theme"].buttons["Escuro"]
        XCTAssertTrue(dark.waitForExistence(timeout: 10))
        let previous = app.segmentedControls["theme"].buttons.allElementsBoundByIndex.first { $0.isSelected }
        dark.tap()
        XCTAssertTrue(dark.isSelected)
        previous?.tap()

        app.buttons["settingsBack"].tap()
        XCTAssertTrue(app.textFields["nickname"].waitForExistence(timeout: 10), "back should return to the connection screen")
    }

    /// The screens are pushed on a `NavigationStack` that follows the Kotlin stack (32.2): the edge swipe goes back
    /// through the shared root, even with each screen's own back button in place of the system one.
    func testSwipingFromTheEdgeGoesBackToTheConnectionScreen() {
        XCTAssertTrue(app.buttons["settingsBack"].waitForExistence(timeout: 10))
        let edge = app.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0.5))
        edge.press(forDuration: 0.1, thenDragTo: app.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5)))

        XCTAssertTrue(app.textFields["nickname"].waitForExistence(timeout: 10), "the swipe should return to the connection screen")
        XCTAssertTrue(app.buttons["settingsBack"].waitForNonExistence(timeout: 5), "the settings should be gone, not just covered")
    }
}
