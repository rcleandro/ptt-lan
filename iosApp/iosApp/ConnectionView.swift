import SwiftUI
import shared

/// State and one-off effects of the `ConnectionComponent`, kept while the screen is shown.
final class ConnectionModel: ObservableObject {
    let component: ConnectionComponent
    @Published private(set) var state: ConnectionState
    @Published private(set) var isCacheEnabled: Bool
    @Published var error: String?
    private var observations: [Observation] = []

    init(component: ConnectionComponent, root: RootComponent) {
        self.component = component
        state = component.state.value as! ConnectionState
        isCacheEnabled = (root.isCacheEnabled.value as? Bool) ?? false
        observations = [
            SwiftBridgeKt.observe(flow: component.state) { [weak self] value in
                if let state = value as? ConnectionState { self?.state = state }
            },
            SwiftBridgeKt.observe(flow: root.isCacheEnabled) { [weak self] value in
                self?.isCacheEnabled = (value as? Bool) ?? false
            },
            SwiftBridgeKt.observe(flow: component.effects) { [weak self] effect in
                guard let effect = effect as? ConnectionEffectShowError else { return }
                Task { @MainActor in
                    self?.error = try? await SwiftBridgeKt.text(resource: effect.message, args: effect.args)
                }
            },
        ]
    }

    deinit {
        observations.forEach { $0.cancel() }
    }

    func send(_ intent: ConnectionIntent) {
        component.onIntent(intent: intent)
    }
}

/// Connection screen in SwiftUI (32.1), over the same `ConnectionComponent` as the Compose one.
struct ConnectionView: View {
    let root: RootComponent
    @StateObject private var model: ConnectionModel
    @StateObject private var texts = Texts([
        "connection_title", "connection_subtitle", "connection_nickname", "connection_pin", "connection_on_network",
        "connection_searching", "connection_search_again", "connection_manual", "connection_manual_hint",
        "connection_connect", "connection_connecting", "common_settings", "common_history",
        "certificate_changed_title", "certificate_trust", "common_cancel", "common_ok",
    ])
    @State private var certificateText = ""

    init(component: ConnectionComponent, root: RootComponent) {
        self.root = root
        _model = StateObject(wrappedValue: ConnectionModel(component: component, root: root))
    }

    private var state: ConnectionState { model.state }
    private var isConnecting: Bool { state.status == ConnectionStatus.connecting }

    @Environment(\.pttTheme) private var theme

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: PttTheme.dimens.spaceLg) {
                    Text(texts["connection_subtitle"])
                        .font(.subheadline)
                        .foregroundStyle(theme.textSecondary)

                    VStack(spacing: 0) {
                        TextField(texts["connection_nickname"], text: binding(state.nickname) { ConnectionIntentUpdateNickname(nickname: $0) })
                            .textContentType(.nickname)
                            .accessibilityIdentifier("nickname")
                            .padding(PttTheme.dimens.spaceXl)
                        Divider().overlay(theme.border)
                        SecureField(texts["connection_pin"], text: binding(state.pin) { ConnectionIntentUpdatePin(pin: $0) })
                            .accessibilityIdentifier("pin")
                            .padding(PttTheme.dimens.spaceXl)
                    }
                    .foregroundStyle(theme.textPrimary)
                    .contentCard()

                    HStack {
                        Text(texts["connection_on_network"].uppercased())
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(theme.textTertiary)
                        if state.discoveredServers.isEmpty {
                            ProgressView().controlSize(.mini)
                            Text(texts["connection_searching"]).font(.caption).foregroundStyle(theme.textTertiary)
                        }
                        Spacer()
                        Button(texts["connection_search_again"], systemImage: "arrow.clockwise") {
                            model.send(ConnectionIntentRefreshServers.shared)
                        }
                        .labelStyle(.iconOnly)
                        .modifier(GlassButtonStyle())
                    }
                    .padding(.top, PttTheme.dimens.spaceMd)

                    if !state.discoveredServers.isEmpty {
                        VStack(spacing: 0) {
                            ForEach(Array(state.discoveredServers.enumerated()), id: \.offset) { index, server in
                                if index > 0 { Divider().overlay(theme.border) }
                                Button {
                                    model.send(ConnectionIntentConnectToDiscovered(server: server))
                                } label: {
                                    ServerRow(server: server)
                                }
                                .buttonStyle(.plain)
                                .disabled(isConnecting)
                            }
                        }
                        .contentCard()
                    }
                }
                .padding(.horizontal, PttTheme.dimens.spaceXl)
            }
            .scrollDismissesKeyboard(.interactively)
            .background(theme.background.ignoresSafeArea())
            .safeAreaInset(edge: .bottom) { manualDock }
            .navigationTitle(texts["connection_title"])
            .toolbar {
                if model.isCacheEnabled {
                    Button(texts["common_history"], systemImage: "clock.arrow.circlepath") { root.navigateToHistory() }
                }
                Button(texts["common_settings"], systemImage: "gearshape") { root.navigateToSettings() }
            }
        }
        .alert(model.error ?? "", isPresented: errorShown) {
            Button(texts["common_ok"], role: .cancel) {}
        }
        .alert(texts["certificate_changed_title"], isPresented: certificateShown) {
            Button(texts["certificate_trust"], role: .destructive) { model.send(ConnectionIntentTrustNewCertificate.shared) }
            Button(texts["common_cancel"], role: .cancel) { model.send(ConnectionIntentDismissCertificateChange.shared) }
        } message: {
            Text(certificateText)
        }
        .task(id: state.certificateChange?.newCode) {
            guard let change = state.certificateChange else { return }
            certificateText = await Texts.format("certificate_changed_text", [change.previousCode, change.newCode])
        }
    }

    /// Manual connection as a floating glass dock, like the Compose `ManualConnectDock`.
    private var manualDock: some View {
        VStack(alignment: .leading, spacing: PttTheme.dimens.spaceSm) {
            Text(texts["connection_manual"])
                .font(.footnote)
                .foregroundStyle(theme.textSecondary)
                .padding(.leading, PttTheme.dimens.spaceSm)
            HStack(spacing: PttTheme.dimens.spaceMd) {
                TextField(texts["connection_manual_hint"], text: binding(state.manualIp) { ConnectionIntentUpdateManualIp(ip: $0) })
                    .font(.body.monospaced())
                    .keyboardType(.URL)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .foregroundStyle(theme.textPrimary)
                    .padding(.horizontal, PttTheme.dimens.spaceXl)
                    .frame(minHeight: PttTheme.dimens.touchTarget)
                    .background(theme.background.opacity(fieldWellOpacity), in: Capsule())
                    .accessibilityIdentifier("manualAddress")
                Button(isConnecting ? texts["connection_connecting"] : texts["connection_connect"]) {
                    model.send(ConnectionIntentConnectToManualIp(ip: state.manualIp))
                }
                .controlSize(.large)
                .modifier(PrimaryGlassButtonStyle())
                .disabled(state.manualIp.isEmpty || isConnecting)
                .accessibilityIdentifier("connect")
            }
        }
        .padding(PttTheme.dimens.spaceLg)
        .glassControl(in: RoundedRectangle(cornerRadius: PttTheme.dimens.dockRadius, style: .continuous))
        .padding(.horizontal, PttTheme.dimens.spaceLg)
        .padding(.bottom, PttTheme.dimens.spaceSm)
    }

    /// A field that shows the component's value and sends every edit back as an intent.
    private func binding(_ value: String, _ intent: @escaping (String) -> ConnectionIntent) -> Binding<String> {
        Binding(get: { value }, set: { model.send(intent($0)) })
    }

    private var errorShown: Binding<Bool> {
        Binding(get: { model.error != nil }, set: { if !$0 { model.error = nil } })
    }

    private var certificateShown: Binding<Bool> {
        Binding(get: { state.certificateChange != nil }, set: { if !$0 { model.send(ConnectionIntentDismissCertificateChange.shared) } })
    }
}

private struct ServerRow: View {
    let server: ServerNode
    @State private var address = ""
    @Environment(\.pttTheme) private var theme

    var body: some View {
        HStack(spacing: PttTheme.dimens.spaceLg) {
            Image(systemName: "antenna.radiowaves.left.and.right")
                .foregroundStyle(theme.primary)
                .frame(width: PttTheme.dimens.touchTarget, height: PttTheme.dimens.touchTarget)
                .background(theme.primaryGlow, in: RoundedRectangle(cornerRadius: PttTheme.dimens.spaceLg, style: .continuous))
            VStack(alignment: .leading, spacing: PttTheme.dimens.spaceXs) {
                Text(server.name).foregroundStyle(theme.textPrimary)
                Text(address).font(.caption.monospaced()).foregroundStyle(theme.textSecondary)
            }
            Spacer()
            Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(theme.textTertiary)
        }
        .padding(PttTheme.dimens.spaceLg)
        .contentShape(Rectangle())
        .task(id: server.endpoint.host) {
            address = await Texts.format("connection_server_address", [server.endpoint.host, server.endpoint.port])
        }
    }
}
