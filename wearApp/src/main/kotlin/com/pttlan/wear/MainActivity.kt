package com.pttlan.wear

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager

/** Placeholder until the watch screens (25.3); keeps the app in the foreground with the screen on. */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
