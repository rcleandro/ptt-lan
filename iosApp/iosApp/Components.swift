import SwiftUI
import shared

/// Soft light behind the glass, in the color of what the screen is about (ADR 0006, `AmbientGlow` in Compose).
/// Off with "Reduzir transparência", like the glass itself.
struct AmbientGlow: View {
    let color: Color
    let intensity: Double
    let size: CGFloat
    @Environment(\.pttTheme) private var theme

    var body: some View {
        if !theme.reduceTransparency {
            Circle()
                .fill(RadialGradient(colors: [color.opacity(intensity), color.opacity(0)], center: .center, startRadius: 0, endRadius: size / 2))
                .frame(width: size, height: size)
                .allowsHitTesting(false)
        }
    }
}

/// Connection badge on a glass capsule: online, still retrying, or offline (the rule is `badgeStatus` in Kotlin).
struct StatusBadge: View {
    let status: Core_designsystemConnectionStatus
    @StateObject private var texts = Texts(["status_online", "status_reconnecting", "status_offline"])
    @Environment(\.pttTheme) private var theme

    var body: some View {
        HStack(spacing: PttTheme.dimens.spaceSm) {
            Circle()
                .strokeBorder(color, lineWidth: status == .reconnecting ? 2 : 0)
                .background(Circle().fill(status == .reconnecting ? .clear : color))
                .frame(width: PttTheme.dimens.spaceMd, height: PttTheme.dimens.spaceMd)
            Text(texts[key])
                .font(.footnote.weight(.medium))
                .foregroundStyle(theme.textPrimary)
        }
        .padding(.horizontal, PttTheme.dimens.spaceLg)
        .frame(height: PttTheme.dimens.glassControl)
        .glassControl(in: Capsule())
    }

    private var color: Color {
        switch status {
        case .online: theme.statusOnline
        case .reconnecting: theme.statusIdle
        default: theme.statusOffline
        }
    }

    private var key: String {
        switch status {
        case .online: "status_online"
        case .reconnecting: "status_reconnecting"
        default: "status_offline"
        }
    }
}
