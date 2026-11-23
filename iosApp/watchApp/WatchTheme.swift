import SwiftUI

/// The dark palette of the design system, as the Wear OS app uses it (25.8). The watch runs no Kotlin, so the values
/// are copied here, each named after its constant in `Color.kt`; `WatchPaletteTest` fails the build when they drift.
enum WatchPalette {
    static let background = Color(hex: 0x0E1114) // BgDark
    static let surface = Color(hex: 0x1B2126) // Surface2Dark
    static let textPrimary = Color(hex: 0xEEF1F4) // TextPrimaryDark
    static let textSecondary = Color(hex: 0xA3ABB3) // TextSecondaryDark
    static let primary = Color(hex: 0x6FA3C0) // PrimaryDark
    static let accentTx = Color(hex: 0xE0915A) // AccentTxDark
    static let statusOnline = Color(hex: 0x6DB39A) // StatusOnlineDark
    static let statusOffline = Color(hex: 0xE27A66) // StatusOfflineDark
    static let iconOnAccentTx = Color(hex: 0x2A160A) // IconOnAccentTxDark
    static let iconOnPrimary = Color(hex: 0x0B1820) // IconOnPrimaryDark
}

/// Spacings of the watch's screens, smaller than the iPhone's `Dimens` for the small screen.
enum WatchDimens {
    static let spaceSm: CGFloat = 4
    static let spaceMd: CGFloat = 8
    static let pressAnimation: Double = 0.12
    /// What is left of a control the user cannot use while the iPhone is out of reach.
    static let disabledOpacity: Double = 0.4
}

private let channelMax: Double = 255

extension Color {
    /// An opaque color from `0xRRGGBB`, the way the palette is written in Kotlin.
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / channelMax,
            green: Double((hex >> 8) & 0xFF) / channelMax,
            blue: Double(hex & 0xFF) / channelMax
        )
    }
}
