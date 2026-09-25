/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 */

package eu.kanade.tachiyomi.ui.player.controls.components.sheets

import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardAlt
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import animiru.domain.player.model.AudioChannels
import animiru.domain.player.model.Decoder
import eu.kanade.presentation.player.components.PlayerSheet
import tachiyomi.domain.custombutton.model.CustomButton
import tachiyomi.i18n.MR
import tachiyomi.i18n.animiru.AMMR
import tachiyomi.i18n.aniyomi.AYMR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreSheet(
    statisticsPage: Int,
    audioChannels: AudioChannels,
    selectedDecoder: Decoder,
    onSelectDecoder: (Decoder) -> Unit,
    remainingTime: Int,
    onStartTimer: (Int) -> Unit,
    onStatisticsPageChange: (Int) -> Unit,
    onCustomButtonClick: (CustomButton) -> Unit,
    onCustomButtonLongClick: (CustomButton) -> Unit,
    onAudioChannelsChange: (AudioChannels) -> Unit,
    onDismissRequest: () -> Unit,
    playbackSpeed: Float,
    onOpenSpeed: () -> Unit,
    onEnterFiltersPanel: () -> Unit,
    onChangeAspect: () -> Unit,
    onCycleRotation: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenQuality: (() -> Unit)?,
    onOpenSubtitleDelay: () -> Unit,
    onOpenAudioDelay: () -> Unit,
    onOpenChapters: (() -> Unit)?,
    onOpenScreenshot: () -> Unit,
    onEnterPip: (() -> Unit)?,
    autoPlayEnabled: Boolean,
    onToggleAutoPlay: (Boolean) -> Unit,
    customButtons: List<CustomButton>,
    modifier: Modifier = Modifier,
) {
    PlayerSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(AYMR.strings.player_sheets_more_title),
                style = MaterialTheme.typography.headlineMedium,
            )

            PlayerSettingsSection(stringResource(AMMR.strings.am_player_group_playback)) {
                FilterChip(
                    selected = false,
                    onClick = onOpenSpeed,
                    label = { Text(stringResource(AYMR.strings.player_speed, playbackSpeed)) },
                )
                FilterChip(
                    selected = autoPlayEnabled,
                    onClick = { onToggleAutoPlay(!autoPlayEnabled) },
                    label = {
                        Text(
                            stringResource(
                                if (autoPlayEnabled) {
                                    AYMR.strings.enable_auto_play
                                } else {
                                    AYMR.strings.disable_auto_play
                                },
                            ),
                        )
                    },
                )
                FilterChip(
                    selected = false,
                    onClick = onOpenSubtitles,
                    label = { Text(stringResource(AYMR.strings.pref_player_subtitle)) },
                )
                FilterChip(
                    selected = false,
                    onClick = onOpenAudio,
                    label = { Text(stringResource(AYMR.strings.pref_player_audio)) },
                )
                if (onOpenQuality != null) {
                    FilterChip(
                        selected = false,
                        onClick = onOpenQuality,
                        label = { Text(stringResource(AYMR.strings.player_sheets_qualities_title)) },
                    )
                }
            }

            PlayerSettingsSection(stringResource(AMMR.strings.am_player_group_video)) {
                FilterChip(
                    selected = false,
                    onClick = onChangeAspect,
                    label = { Text(stringResource(AMMR.strings.am_player_action_aspect_ratio)) },
                )
                FilterChip(
                    selected = false,
                    onClick = onCycleRotation,
                    label = { Text(stringResource(AYMR.strings.pref_category_player_orientation)) },
                )
                FilterChip(
                    selected = false,
                    onClick = onEnterFiltersPanel,
                    label = { Text(stringResource(AYMR.strings.player_sheets_filters_title)) },
                )
            }

            PlayerSettingsSection(stringResource(AMMR.strings.am_player_group_timing)) {
                FilterChip(
                    selected = false,
                    onClick = onOpenSubtitleDelay,
                    label = { Text(stringResource(AYMR.strings.player_sheets_sub_delay_title)) },
                )
                FilterChip(
                    selected = false,
                    onClick = onOpenAudioDelay,
                    label = { Text(stringResource(AYMR.strings.player_sheets_audio_delay_title)) },
                )
                if (onOpenChapters != null) {
                    FilterChip(
                        selected = false,
                        onClick = onOpenChapters,
                        label = { Text(stringResource(AYMR.strings.player_sheets_chapters_title)) },
                    )
                }

                var isSleepTimerDialogShown by remember { mutableStateOf(false) }
                FilterChip(
                    selected = remainingTime > 0,
                    onClick = { isSleepTimerDialogShown = true },
                    label = {
                        Text(
                            if (remainingTime == 0) {
                                stringResource(AYMR.strings.timer_title)
                            } else {
                                stringResource(
                                    AYMR.strings.timer_remaining,
                                    DateUtils.formatElapsedTime(remainingTime.toLong()),
                                )
                            },
                        )
                    },
                )
                if (isSleepTimerDialogShown) {
                    TimePickerDialog(
                        remainingTime = remainingTime,
                        onDismissRequest = { isSleepTimerDialogShown = false },
                        onTimeSelect = onStartTimer,
                    )
                }
            }

            PlayerSettingsSection(stringResource(AMMR.strings.am_player_group_tools)) {
                FilterChip(
                    selected = false,
                    onClick = onOpenScreenshot,
                    label = { Text(stringResource(AMMR.strings.am_player_action_screenshot)) },
                )
                if (onEnterPip != null) {
                    FilterChip(
                        selected = false,
                        onClick = onEnterPip,
                        label = { Text(stringResource(AYMR.strings.pref_category_pip)) },
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                Text(
                    text = stringResource(AMMR.strings.am_player_group_advanced),
                    style = MaterialTheme.typography.titleMedium,
                )

                Text(stringResource(AYMR.strings.player_hwdec_mode))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                    items(Decoder.entries.minus(Decoder.Auto)) { decoder ->
                        FilterChip(
                            selected = decoder == selectedDecoder,
                            onClick = { onSelectDecoder(decoder) },
                            label = { Text(text = decoder.title) },
                        )
                    }
                }

                Text(stringResource(AYMR.strings.player_sheets_stats_page_title))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                    items(6) { page ->
                        FilterChip(
                            label = {
                                Text(
                                    stringResource(
                                        if (page == 0) {
                                            AYMR.strings.player_sheets_tracks_off
                                        } else {
                                            AYMR.strings.player_sheets_stats_page_chip
                                        },
                                        page,
                                    ),
                                )
                            },
                            onClick = { onStatisticsPageChange(page) },
                            selected = statisticsPage == page,
                        )
                    }
                }

                if (customButtons.isNotEmpty()) {
                    Text(text = stringResource(AYMR.strings.player_sheets_custom_buttons_title))
                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.mediumSmall),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        maxItemsInEachRow = Int.MAX_VALUE,
                    ) {
                        customButtons.forEach { button ->
                            val interactionSource = remember { MutableInteractionSource() }
                            Box {
                                FilterChip(
                                    onClick = {},
                                    label = { Text(text = button.name) },
                                    selected = false,
                                    interactionSource = interactionSource,
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .combinedClickable(
                                            onClick = { onCustomButtonClick(button) },
                                            onLongClick = { onCustomButtonLongClick(button) },
                                            interactionSource = interactionSource,
                                            indication = null,
                                        ),
                                )
                            }
                        }
                    }
                }

                Text(text = stringResource(AYMR.strings.pref_audio_channels))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                    items(AudioChannels.entries) { channels ->
                        FilterChip(
                            selected = audioChannels == channels,
                            onClick = { onAudioChannelsChange(channels) },
                            label = { Text(text = stringResource(channels.titleRes)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerSettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        FlowRow(
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        ) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    remainingTime: Int = 0,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier.padding(MaterialTheme.padding.medium),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .width(IntrinsicSize.Max)
                    .animateContentSize()
                    .padding(MaterialTheme.padding.medium),
            ) {
                var currentLayoutType by rememberSaveable { mutableIntStateOf(0) }
                Text(
                    text = stringResource(
                        if (currentLayoutType == 1) {
                            AYMR.strings.timer_picker_pick_time
                        } else {
                            AYMR.strings.timer_picker_enter_timer
                        },
                    ),
                )

                val state = rememberTimePickerState(
                    remainingTime / 3600,
                    (remainingTime % 3600) / 60,
                    is24Hour = true,
                )
                Box(
                    contentAlignment = Alignment.Center,
                ) {
                    if (currentLayoutType == 1) {
                        TimePicker(state = state)
                    } else {
                        TimeInput(state = state)
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    IconButton(onClick = { currentLayoutType = if (currentLayoutType == 0) 1 else 0 }) {
                        Icon(
                            imageVector = if (currentLayoutType ==
                                0
                            ) {
                                Icons.Outlined.Schedule
                            } else {
                                Icons.Default.KeyboardAlt
                            },
                            contentDescription = null,
                        )
                    }
                    Row {
                        if (remainingTime > 0) {
                            TextButton(onClick = {
                                onTimeSelect(0)
                                onDismissRequest()
                            }) {
                                Text(stringResource(AYMR.strings.timer_cancel_timer))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onTimeSelect(state.hour * 3600 + state.minute * 60)
                                onDismissRequest()
                            },
                        ) {
                            Text(stringResource(MR.strings.action_ok))
                        }
                    }
                }
            }
        }
    }
}
