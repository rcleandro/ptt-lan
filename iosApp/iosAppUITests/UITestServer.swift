import XCTest

extension XCTestCase {
    /// Connects [app] to a PTT-LAN server, with the nickname already typed (32.5). With `PTT_SERVER_ADDRESS` set (CI
    /// starts a server next to the simulator) it types that address, and a server that never answers fails the
    /// test; without it, it picks the first server Bonjour finds and skips where none answers.
    func connectToServer(_ app: XCUIApplication) throws {
        if let address = ProcessInfo.processInfo.environment["PTT_SERVER_ADDRESS"], !address.isEmpty {
            let field = app.textFields["manualAddress"]
            XCTAssertTrue(field.waitForExistence(timeout: 10))
            field.tap()
            field.typeText(address)
            app.buttons["connect"].tap()
            return
        }
        let server = app.buttons.matching(NSPredicate(format: "label BEGINSWITH 'PTT-LAN'")).firstMatch
        guard server.waitForExistence(timeout: 8) else {
            throw XCTSkip("no PTT-LAN server found on this network")
        }
        server.tap()
    }
}
