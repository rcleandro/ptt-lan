package com.pttlan.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pttlan.core.designsystem.theme.PttTheme

@Composable
fun ChannelCard(
    name: String,
    participantCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = participantCount == 0
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .contentCard()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "# $name",
                style = MaterialTheme.typography.titleMedium,
                color = if (isEmpty) secondary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = PttTheme.customColors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text =
                        when (participantCount) {
                            0 -> "vazia"
                            1 -> "1 pessoa"
                            else -> "$participantCount pessoas"
                        },
                    style = MaterialTheme.typography.labelSmall,
                    color = secondary,
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = PttTheme.customColors.textTertiary,
        )
    }
}
