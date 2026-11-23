package com.pttlan.shared

import androidx.compose.ui.window.ComposeUIViewController
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import platform.UIKit.UIViewController

/**
 * The Compose screens not migrated to SwiftUI yet (32.4), over the same [root] as the SwiftUI ones. The screens
 * SwiftUI draws stay empty here.
 */
@Suppress("FunctionName")
fun MainViewController(root: RootComponent): UIViewController =
    ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        },
    ) {
        RootScreen(component = root, isNative = ::isNativeScreen)
    }

/** Screens drawn in SwiftUI, the rest is still Compose: the Swift root asks this too, so both agree. */
fun isNativeScreen(child: RootComponent.Child): Boolean = child is RootComponent.Child.ConnectionChild
