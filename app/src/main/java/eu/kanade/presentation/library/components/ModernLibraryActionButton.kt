package eu.kanade.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.theme.LocalModernArtworkIdentity
import eu.kanade.presentation.theme.LocalModernVisualPalette
import eu.kanade.presentation.theme.ModernLibraryGeometry
import eu.kanade.presentation.theme.modernLibraryActionSurface
import eu.kanade.presentation.theme.resolveModernLibraryChrome

@Composable
internal fun ModernLibraryActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val chrome = requireNotNull(
        resolveModernLibraryChrome(LocalModernVisualPalette.current, LocalModernArtworkIdentity.current),
    )
    val contentColor = if (selected) chrome.selectedContent else chrome.content
    val interactions = remember { MutableInteractionSource() }

    // One rectangular owner handles the entire touch/focus/semantics area. The visual child
    // has no click handler and can grow with wrapped or scaled text independently of that minimum.
    Box(
        modifier = modifier
            .heightIn(min = ModernLibraryGeometry.TouchTarget)
            .semantics(mergeDescendants = true) { this.selected = selected }
            .clickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ModernLibraryGeometry.FilterHeight)
                .modernLibraryActionSurface(chrome, selected = selected)
                .clip(chrome.actionShape)
                .indication(interactions, ripple(color = contentColor))
                .padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(ModernLibraryGeometry.FilterIcon),
            )
            Text(
                text = label,
                color = contentColor,
                modifier = Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                fontSize = ModernLibraryGeometry.FilterText,
                lineHeight = ModernLibraryGeometry.FilterLineHeight,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
