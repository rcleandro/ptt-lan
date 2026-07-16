package com.pttlan.core.designsystem.components.snackbar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme

private const val SNACKBAR_TEXT_MAX_LINES_COUNT = 3

@Composable
private fun BaseNewSnackbar(
    text: String,
    type: PttSnackbarType,
) {
    val containerColor =
        when (type) {
            PttSnackbarType.Success -> PttTheme.customColors.statusOnline
            PttSnackbarType.ErrorOrWarning -> PttTheme.customColors.statusOffline
            PttSnackbarType.Generic -> PttTheme.customColors.surface2
        }

    val contentColor = MaterialTheme.colorScheme.onBackground

    Snackbar(
        modifier = Modifier.padding(16.dp),
        containerColor = containerColor,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            type.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = "Icon",
                    tint = contentColor,
                    modifier = Modifier.size(24.dp),
                )

                Spacer(Modifier.width(8.dp))
            }

            Text(
                text = text,
                color = contentColor,
                maxLines = SNACKBAR_TEXT_MAX_LINES_COUNT,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun PttSnackbarHost(
    state: SnackbarHostState,
    type: PttSnackbarType,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = state, modifier = modifier) { data: SnackbarData ->
        BaseNewSnackbar(
            text = data.visuals.message,
            type = type,
        )
    }
}
