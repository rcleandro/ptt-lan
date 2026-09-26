package com.pttlan.desktop

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.app_name
import com.pttlan.core.di.appModules
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.pttlan.domain.ptt.repository.LocalServerHost
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.awt.Taskbar
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

fun main() {
    val serverHost = DesktopServerHost()
    startKoin {
        modules(appModules() + module { single<LocalServerHost> { serverHost } })
    }

    val lifecycle = LifecycleRegistry()
    val componentContext = DefaultComponentContext(lifecycle)

    var rootComponent: RootComponent? = null
    SwingUtilities.invokeAndWait {
        rootComponent =
            RootComponent(
                componentContext = componentContext,
            )
    }

    // From the classpath: `./gradlew run` has no bundle, so the installer's iconFile does not apply to it.
    val icon = object {}.javaClass.getResource("/icon.png")?.let(ImageIO::read)
    // On macOS the Dock keeps the JVM's icon unless set here; the window icon alone does not change it.
    if (icon != null && Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
        Taskbar.getTaskbar().iconImage = icon
    }

    application {
        // Netty threads are not daemons: without stopping the host, closing the window would leave the process alive
        Window(onCloseRequest = {
            serverHost.stop()
            exitApplication()
        }, title = stringResource(Res.string.app_name), icon = icon?.let { BitmapPainter(it.toComposeImageBitmap()) }) {
            RootScreen(component = rootComponent!!)
        }
    }
}
