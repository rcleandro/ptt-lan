import SwiftUI
import shared

// Ambient light, the same as the Compose screen: blue from the top corner
private let glowSize: CGFloat = 440
private let glowOffset = CGSize(width: 180, height: -140)
private let glowIntensity = 0.24

/// State of the `SettingsComponent`, kept while the screen is shown.
final class SettingsModel: ObservableObject {
    let component: SettingsComponent
    @Published private(set) var state: SettingsState
    private var observation: Observation?

    init(component: SettingsComponent) {
        self.component = component
        state = component.state.value as! SettingsState
        observation = SwiftBridgeKt.observe(flow: component.state) { [weak self] value in
            if let state = value as? SettingsState { self?.state = state }
        }
    }

    deinit {
        observation?.cancel()
    }

    func send(_ intent: SettingsIntent) {
        component.onIntent(intent: intent)
    }
}

/// Settings in SwiftUI (32.4), over the same `SettingsComponent` as the Compose screen. Sizes, storage names, the
/// usage bar and the slider bounds come from `SettingsRules.kt`.
struct SettingsView: View {
    let root: RootComponent
    @StateObject private var model: SettingsModel
    @StateObject private var texts = Texts([
        "settings_title", "settings_back", "settings_appearance", "settings_theme", "settings_theme_system",
        "settings_theme_light", "settings_theme_dark", "settings_reduce_transparency",
        "settings_reduce_transparency_detail", "settings_audio", "settings_opus", "settings_opus_detail",
        "settings_headset_mic", "settings_headset_mic_detail", "settings_always_listening",
        "settings_always_listening_detail", "settings_history", "settings_save_audio", "settings_location",
        "settings_space_limit", "settings_location_title", "settings_sd_unavailable", "settings_clear_history",
        "settings_clear_history_text", "settings_clear_confirm", "common_cancel",
    ])
    @State private var choosingLocation = false
    @State private var confirmingClear = false
    @Environment(\.pttTheme) private var theme

    init(component: SettingsComponent, root: RootComponent) {
        self.root = root
        _model = StateObject(wrappedValue: SettingsModel(component: component))
    }

    private var state: SettingsState { model.state }

    var body: some View {
        Group {
            ScrollView {
                VStack(alignment: .leading, spacing: PttTheme.dimens.spaceLg) {
                    Text(texts["settings_title"])
                        .font(.largeTitle.bold())
                        .foregroundStyle(theme.textPrimary)
                    appearance
                    audio
                    history
                }
                .padding(.horizontal, PttTheme.dimens.space2xl)
                .padding(.bottom, PttTheme.dimens.space2xl)
                .readableWidth()
            }
            .background {
                ZStack {
                    theme.background
                    AmbientGlow(color: theme.primary, intensity: glowIntensity, size: glowSize)
                        .offset(glowOffset)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
                }
                .ignoresSafeArea()
            }
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(texts["settings_back"], systemImage: "chevron.backward") { root.goBack() }
                        .accessibilityIdentifier("settingsBack")
                }
            }
        }
        .confirmationDialog(texts["settings_location_title"], isPresented: $choosingLocation, titleVisibility: .visible) {
            ForEach(state.storageOptions, id: \.id) { option in
                LocationOptionButton(option: option) { model.send(SettingsIntentChangeCacheLocation(location: option.id)) }
            }
        } message: {
            if state.isSdCardMissing { Text(texts["settings_sd_unavailable"]) }
        }
        .alert(texts["settings_clear_history"], isPresented: $confirmingClear) {
            Button(texts["settings_clear_confirm"], role: .destructive) { model.send(SettingsIntentClearCache.shared) }
            Button(texts["common_cancel"], role: .cancel) {}
        } message: {
            Text(texts["settings_clear_history_text"])
        }
    }

    private var appearance: some View {
        SettingsGroup(title: texts["settings_appearance"]) {
            VStack(alignment: .leading, spacing: PttTheme.dimens.spaceMd) {
                Text(texts["settings_theme"]).foregroundStyle(theme.textPrimary)
                Picker(texts["settings_theme"], selection: Binding(
                    get: { state.appTheme },
                    set: { model.send(SettingsIntentChangeTheme(theme: $0)) }
                )) {
                    Text(texts["settings_theme_system"]).tag(Core_designsystemAppTheme.system)
                    Text(texts["settings_theme_light"]).tag(Core_designsystemAppTheme.light)
                    Text(texts["settings_theme_dark"]).tag(Core_designsystemAppTheme.dark)
                }
                .pickerStyle(.segmented)
                .accessibilityIdentifier("theme")
            }
            .padding(PttTheme.dimens.spaceXl)
            GroupDivider()
            SwitchRow(
                title: texts["settings_reduce_transparency"],
                subtitle: texts["settings_reduce_transparency_detail"],
                isOn: state.reduceTransparency
            ) { model.send(SettingsIntentToggleReduceTransparency(enabled: $0)) }
        }
    }

    private var audio: some View {
        SettingsGroup(title: texts["settings_audio"]) {
            SwitchRow(title: texts["settings_opus"], subtitle: texts["settings_opus_detail"], isOn: state.useOpus) {
                model.send(SettingsIntentToggleOpus(enabled: $0))
            }
            GroupDivider()
            SwitchRow(title: texts["settings_headset_mic"], subtitle: texts["settings_headset_mic_detail"], isOn: state.useHeadsetMic) {
                model.send(SettingsIntentToggleHeadsetMic(enabled: $0))
            }
            GroupDivider()
            SwitchRow(
                title: texts["settings_always_listening"],
                subtitle: texts["settings_always_listening_detail"],
                isOn: state.alwaysListening
            ) { model.send(SettingsIntentToggleAlwaysListening(enabled: $0)) }
        }
    }

    @ViewBuilder
    private var history: some View {
        SettingsGroup(title: texts["settings_history"]) {
            SwitchRow(title: texts["settings_save_audio"], subtitle: nil, isOn: state.allowCache) {
                model.send(SettingsIntentToggleAllowCache(enabled: $0))
            }
            .accessibilityIdentifier("saveAudio")
            if state.allowCache {
                GroupDivider()
                Button { choosingLocation = true } label: {
                    HStack(spacing: PttTheme.dimens.spaceMd) {
                        Text(texts["settings_location"]).foregroundStyle(theme.textPrimary)
                        Spacer()
                        ResourceText(resource: SettingsRulesKt.storageName(id: state.cacheLocation))
                            .foregroundStyle(theme.textSecondary)
                        Image(systemName: "chevron.right").font(.footnote.weight(.semibold)).foregroundStyle(theme.textTertiary)
                    }
                    .padding(PttTheme.dimens.spaceXl)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                GroupDivider()
                CacheLimit(state: state, title: texts["settings_space_limit"]) {
                    model.send(SettingsIntentChangeMaxCacheSize(sizeMb: $0))
                }
            }
        }
        .animation(.default, value: state.allowCache)
        if state.allowCache {
            Button(texts["settings_clear_history"], systemImage: "trash", role: .destructive) { confirmingClear = true }
                .frame(maxWidth: .infinity)
                .controlSize(.large)
                .modifier(GlassButtonStyle())
                .foregroundStyle(theme.statusOffline)
                .padding(.top, PttTheme.dimens.spaceSm)
        }
    }
}

/// A labelled card of settings, the content layer of ADR 0006.
private struct SettingsGroup<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content
    @Environment(\.pttTheme) private var theme

    var body: some View {
        Text(title.uppercased())
            .font(.caption.weight(.semibold))
            .foregroundStyle(theme.textTertiary)
            .padding(.leading, PttTheme.dimens.spaceXl)
            .padding(.top, PttTheme.dimens.spaceMd)
        VStack(alignment: .leading, spacing: 0) { content }
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentCard()
    }
}

private struct GroupDivider: View {
    @Environment(\.pttTheme) private var theme

    var body: some View {
        Divider().overlay(theme.border).padding(.leading, PttTheme.dimens.spaceXl)
    }
}

/// A setting that is on or off, with what it does under the title.
private struct SwitchRow: View {
    let title: String
    let subtitle: String?
    let isOn: Bool
    let onChange: (Bool) -> Void
    @Environment(\.pttTheme) private var theme

    var body: some View {
        Toggle(isOn: Binding(get: { isOn }, set: onChange)) {
            VStack(alignment: .leading, spacing: PttTheme.dimens.spaceXs) {
                Text(title).foregroundStyle(theme.textPrimary)
                if let subtitle {
                    Text(subtitle).font(.footnote).foregroundStyle(theme.textSecondary)
                }
            }
        }
        .padding(.horizontal, PttTheme.dimens.spaceXl)
        .padding(.vertical, PttTheme.dimens.spaceLg)
    }
}

/// The history limit: a slider in the steps of `SettingsRules.kt` and how much of it is in use.
private struct CacheLimit: View {
    let state: SettingsState
    let title: String
    let onChange: (Int32) -> Void
    @State private var limitText = ""
    @State private var usageText = ""
    @Environment(\.pttTheme) private var theme

    var body: some View {
        VStack(alignment: .leading, spacing: PttTheme.dimens.spaceMd) {
            HStack {
                Text(title).foregroundStyle(theme.textPrimary)
                Spacer()
                Text(limitText).font(.footnote.weight(.medium)).foregroundStyle(theme.primary)
            }
            Slider(
                value: Binding(get: { Double(state.maxCacheSizeMb) }, set: { onChange(Int32($0.rounded())) }),
                in: Double(SettingsRulesKt.CACHE_SIZE_MIN_MB)...Double(SettingsRulesKt.CACHE_SIZE_MAX_MB),
                step: Double(SettingsRulesKt.CACHE_SIZE_STEP_MB)
            )
            .accessibilityIdentifier("cacheLimit")
            ProgressView(value: Double(state.cacheUsage()))
                .tint(theme.primary)
            Text(usageText.uppercased()).font(.caption.weight(.semibold)).foregroundStyle(theme.textTertiary)
        }
        .padding(PttTheme.dimens.spaceXl)
        .task(id: "\(state.maxCacheSizeMb)-\(state.currentCacheUsageMb)") {
            limitText = await Texts.format("settings_size_mb", [state.maxCacheSizeMb])
            usageText = await Texts.format("settings_usage", [state.currentCacheUsageMb, state.maxCacheSizeMb])
        }
    }
}

/// "Interno (12 GB livre)": the place's name and its free space, as the Compose dialog writes them.
private struct LocationOptionButton: View {
    let option: Core_commonStorageOption
    let onSelect: () -> Void
    @State private var label = ""

    var body: some View {
        Button(label, action: onSelect).task(id: option.id) {
            let size = SettingsRulesKt.sizeText(bytes: option.availableSpaceBytes)
            let name = (try? await SwiftBridgeKt.text(resource: SettingsRulesKt.storageName(id: option.id), args: [])) ?? ""
            let free = (try? await SwiftBridgeKt.text(resource: size.resource, args: [size.value])) ?? ""
            label = await Texts.format("settings_location_option", [name, free])
        }
    }
}
