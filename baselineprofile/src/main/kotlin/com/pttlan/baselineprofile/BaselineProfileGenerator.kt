package com.pttlan.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val PACKAGE = "com.pttlan.android"
private const val UI_TIMEOUT_MS = 10_000L

/**
 * The path every session takes: the app opens, the phone hosts a room (no server needed on the network) and the
 * PTT screen of the default channel opens. The labels are the pt-BR ones the screens show.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndPttScreen() =
        rule.collect(packageName = PACKAGE, includeInStartupProfile = true) {
            device.executeShellCommand("pm grant $PACKAGE android.permission.RECORD_AUDIO")
            device.executeShellCommand("pm grant $PACKAGE android.permission.POST_NOTIFICATIONS")
            pressHome()
            startActivityAndWait()

            device.wait(Until.findObject(By.text("Hospedar")), UI_TIMEOUT_MS).click()
            device.wait(Until.findObject(By.text("# Geral")), UI_TIMEOUT_MS).click()
            device.wait(Until.hasObject(By.text("Segure para falar")), UI_TIMEOUT_MS)
        }
}
