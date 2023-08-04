// AM (NAVPILL) -->
package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun NavigationPill(
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
) {
    val tabNavigator = LocalTabNavigator.current
    val currTabIndex = tabNavigator.current.options.index.toInt()
    val tabMap = tabs.associateBy { it.options.index.toInt() }

    val updateTab: (Int) -> Unit = {
        tabNavigator.current = tabMap[it] ?: tabNavigator.current
    }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
        var offsetX by remember { mutableStateOf(0f) }
        var currentIndex by remember { mutableStateOf(currTabIndex) }

        Row(
            modifier = modifier
                .selectableGroup()
                .navigationBarsPadding()
                .padding(bottom = MaterialTheme.padding.large)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                        },
                        onDragEnd = {
                            currentIndex = when {
                                (offsetX < 0F) -> {
                                    currentIndex - 1
                                }
                                (offsetX > 0F) -> {
                                    currentIndex + 1
                                }
                                else -> currentIndex
                            }
                            offsetX = 0F

                            updateTab(minOf(maxOf(currentIndex, 0), 4))
                        },
                    )
                },
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            tabs.fastForEach {
                NavigationBarItem(it)
            }
        }
    }
}

@Composable
private fun NavigationBarItem(tab: Tab) {
    val tabNavigator = LocalTabNavigator.current
    val navigator = LocalNavigator.currentOrThrow
    val configuration = LocalConfiguration.current

    val scope = rememberCoroutineScope()
    val selected = tabNavigator.current::class == tab::class
    val onClick: () -> Unit = {
        if (!selected) {
            tabNavigator.current = tab
        } else {
            scope.launch { tab.onReselect(navigator) }
        }
    }

    val backgroundAlpha: Float by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(500),
    )

    Box(
        modifier = Modifier
            .size(width = (configuration.screenWidthDp / 6).dp, height = 48.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            )
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = backgroundAlpha)),
        contentAlignment = Alignment.Center,
    ) {
        NavigationIconItem(tab)
    }
}

@Composable
private fun NavigationIconItem(tab: Tab) {
    BadgedBox(
        badge = {
            when {
                UpdatesTab::class.isInstance(tab) -> {
                    val count by produceState(initialValue = 0) {
                        val pref = Injekt.get<LibraryPreferences>()
                        pref.newAnimeUpdatesCount().changes()
                            .collectLatest { value = if (pref.newShowUpdatesCount().get()) it else 0 }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                id = R.plurals.notification_episodes_generic,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }
                BrowseTab::class.isInstance(tab) -> {
                    val count by produceState(initialValue = 0) {
                        val pref = Injekt.get<SourcePreferences>()
                        pref.animeExtensionUpdatesCount().changes()
                            .collectLatest { value = it }
                    }
                    if (count > 0) {
                        Badge {
                            val desc = pluralStringResource(
                                id = R.plurals.update_check_notification_ext_updates,
                                count = count,
                                count,
                            )
                            Text(
                                text = count.toString(),
                                modifier = Modifier.semantics { contentDescription = desc },
                            )
                        }
                    }
                }
            }
        },
    ) {
        Icon(
            painter = tab.options.icon!!,
            contentDescription = tab.options.title,
            modifier = Modifier.size(28.dp),
        )
    }
}

// <-- AM (NAVPILL)
