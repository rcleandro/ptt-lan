package com.pttlan.wear

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.wear.input.RemoteInputIntentHelper

private const val TEXT_KEY = "text"

/** Opens the system's text entry (keyboard, voice or handwriting) and hands back what was typed. */
@Composable
fun rememberTextInput(
    label: String,
    onResult: (String) -> Unit,
): () -> Unit {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val text = result.data?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(TEXT_KEY) }
            if (text != null) onResult(text.toString())
        }
    return {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(RemoteInput.Builder(TEXT_KEY).setLabel(label).build()))
        launcher.launch(intent)
    }
}
