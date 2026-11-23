import SwiftUI
import shared

extension Color {
    /// A design-system token, which Kotlin hands over as ARGB.
    init(argb: Int32) {
        let bits = UInt32(bitPattern: argb)
        self.init(
            .sRGB,
            red: Double((bits >> 16) & 0xFF) / 255,
            green: Double((bits >> 8) & 0xFF) / 255,
            blue: Double(bits & 0xFF) / 255,
            opacity: Double(bits >> 24) / 255
        )
    }
}

/// A text field sits in a darker well inside a glass dock, so it reads as a field and not as more glass.
let fieldWellOpacity = 0.6

/// The design system for SwiftUI screens: the same palette, radii and spacings as Compose (ADR 0006, 32.3).
struct PttTheme {
    let palette: SwiftPalette
    let reduceTransparency: Bool

    var background: Color { Color(argb: palette.background) }
    var surface: Color { Color(argb: palette.surface) }
    var border: Color { Color(argb: palette.border) }
    var textPrimary: Color { Color(argb: palette.textPrimary) }
    var textSecondary: Color { Color(argb: palette.textSecondary) }
    var textTertiary: Color { Color(argb: palette.textTertiary) }
    var primary: Color { Color(argb: palette.primary) }
    var primaryGlow: Color { Color(argb: palette.primaryGlow) }
    var onPrimary: Color { Color(argb: palette.onPrimary) }
    var accentTx: Color { Color(argb: palette.accentTx) }
    var accentTxGlow: Color { Color(argb: palette.accentTxGlow) }
    var onAccentTx: Color { Color(argb: palette.onAccentTx) }
    var statusOnline: Color { Color(argb: palette.statusOnline) }
    var statusOffline: Color { Color(argb: palette.statusOffline) }
    var statusIdle: Color { Color(argb: palette.statusIdle) }

    static let dimens = SwiftDimens.shared
}

private struct PttThemeKey: EnvironmentKey {
    static let defaultValue = PttTheme(palette: DesignTokensKt.swiftPalette(dark: true), reduceTransparency: false)
}

extension EnvironmentValues {
    var pttTheme: PttTheme {
        get { self[PttThemeKey.self] }
        set { self[PttThemeKey.self] = newValue }
    }
}

/// The user's theme and "Reduzir transparência" from settings, which live in the navigation root.
final class ThemeModel: ObservableObject {
    @Published private(set) var appTheme: Core_designsystemAppTheme
    @Published private(set) var reduceTransparency: Bool
    private var observations: [Observation] = []

    init(root: RootComponent) {
        appTheme = root.appTheme.value as! Core_designsystemAppTheme
        reduceTransparency = (root.reduceTransparency.value as? Bool) ?? false
        observations = [
            SwiftBridgeKt.observe(flow: root.appTheme) { [weak self] value in
                if let theme = value as? Core_designsystemAppTheme { self?.appTheme = theme }
            },
            SwiftBridgeKt.observe(flow: root.reduceTransparency) { [weak self] value in
                self?.reduceTransparency = (value as? Bool) ?? false
            },
        ]
    }

    deinit {
        observations.forEach { $0.cancel() }
    }

    /// Light or dark forced in settings; `nil` follows the system.
    var colorScheme: ColorScheme? {
        switch appTheme {
        case .light: .light
        case .dark: .dark
        default: nil
        }
    }
}

/// Applies the design system under [content], following the theme chosen in settings.
struct PttThemed<Content: View>: View {
    @ObservedObject var model: ThemeModel
    @ViewBuilder let content: Content
    @Environment(\.colorScheme) private var systemScheme

    var body: some View {
        let dark = DesignTokensKt.isDarkTheme(theme: model.appTheme, systemIsDark: systemScheme == .dark)
        content
            .environment(\.pttTheme, PttTheme(palette: DesignTokensKt.swiftPalette(dark: dark), reduceTransparency: model.reduceTransparency))
            .tint(Color(argb: DesignTokensKt.swiftPalette(dark: dark).primary))
            .preferredColorScheme(model.colorScheme)
    }
}

extension View {
    /// Content layer (lists, cards): a solid surface, never glass (ADR 0006).
    func contentCard() -> some View {
        modifier(ContentCard())
    }

    /// Control layer (bars, docks, pills): Liquid Glass on iOS 26, translucent material before it, and a solid
    /// surface with "Reduzir transparência" (ADR 0006).
    func glassControl<S: Shape>(in shape: S) -> some View {
        modifier(GlassControl(shape: shape))
    }
}

private struct ContentCard: ViewModifier {
    @Environment(\.pttTheme) private var theme

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: PttTheme.dimens.cardRadius, style: .continuous)
        content
            .background(theme.surface, in: shape)
            .overlay(shape.strokeBorder(theme.border, lineWidth: 1))
    }
}

private struct GlassControl<S: Shape>: ViewModifier {
    let shape: S
    @Environment(\.pttTheme) private var theme

    func body(content: Content) -> some View {
        if theme.reduceTransparency {
            content.background(theme.surface, in: shape).overlay(shape.stroke(theme.border, lineWidth: 1))
        } else if #available(iOS 26, *) {
            content.glassEffect(.regular, in: shape)
        } else {
            content.background(.ultraThinMaterial, in: shape)
        }
    }
}

/// Primary action as a glass capsule tinted with the action color (`.glassProminent` on iOS 26).
struct PrimaryGlassButtonStyle: ViewModifier {
    @Environment(\.pttTheme) private var theme

    func body(content: Content) -> some View {
        if #available(iOS 26, *), !theme.reduceTransparency {
            content.buttonStyle(.glassProminent).tint(theme.primary)
        } else {
            content.buttonStyle(.borderedProminent).buttonBorderShape(.capsule).tint(theme.primary)
        }
    }
}

/// Secondary action as a clear glass capsule (`.glass` on iOS 26).
struct GlassButtonStyle: ViewModifier {
    @Environment(\.pttTheme) private var theme

    func body(content: Content) -> some View {
        if #available(iOS 26, *), !theme.reduceTransparency {
            content.buttonStyle(.glass)
        } else {
            content.buttonStyle(.bordered).buttonBorderShape(.capsule)
        }
    }
}
