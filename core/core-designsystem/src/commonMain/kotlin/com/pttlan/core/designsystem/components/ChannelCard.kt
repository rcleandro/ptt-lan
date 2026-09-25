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
import com.pttlan.core.designsystem.generated.resources.Res
import com.pttlan.core.designsystem.generated.resources.channel_card_empty
import com.pttlan.core.designsystem.generated.resources.channel_card_people
import com.pttlan.core.designsystem.generated.resources.channel_name
import com.pttlan.core.designsystem.theme.Dimens
import com.pttlan.core.designsystem.theme.PttTheme
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private val CardPaddingVertical = 14.dp
private val PeopleIconSize = 14.dp

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
                .padding(horizontal = Dimens.SpaceXl, vertical = CardPaddingVertical),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceLg),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
            Text(
                text = stringResource(Res.string.channel_name, name),
                style = MaterialTheme.typography.titleMedium,
                color = if (isEmpty) secondary else MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceSm)) {
                Icon(
                    imageVector = Icons.Default.People,
                    contentDescription = null,
                    tint = PttTheme.customColors.textTertiary,
                    modifier = Modifier.size(PeopleIconSize),
                )
                Text(
                    text =
                        if (isEmpty) {
                            stringResource(Res.string.channel_card_empty)
                        } else {
                            pluralStringResource(Res.plurals.channel_card_people, participantCount, participantCount)
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
