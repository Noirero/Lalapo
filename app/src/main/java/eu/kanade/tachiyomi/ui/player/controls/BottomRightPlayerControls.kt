/*
 * Copyright 2024 Abdallah Mehiz
 * https://github.com/abdallahmehiz/mpvKt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.controls.components.ControlsButton
import tachiyomi.presentation.core.components.material.padding

@Composable
fun BottomRightPlayerControls(
    showSubtitles: Boolean,
    showAudio: Boolean,
    showQuality: Boolean,
    isPipAvailable: Boolean,
    onSubtitlesClick: () -> Unit,
    onSubtitlesLongClick: () -> Unit,
    onAudioClick: () -> Unit,
    onAudioLongClick: () -> Unit,
    onQualityClick: () -> Unit,
    onAspectClick: () -> Unit,
    onPipClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        if (showSubtitles) {
            ControlsButton(
                icon = Icons.Default.Subtitles,
                onClick = onSubtitlesClick,
                onLongClick = onSubtitlesLongClick,
                verticalSpacing = MaterialTheme.padding.small,
            )
        }

        if (showAudio) {
            ControlsButton(
                icon = Icons.Default.Audiotrack,
                onClick = onAudioClick,
                onLongClick = onAudioLongClick,
                verticalSpacing = MaterialTheme.padding.small,
            )
        }

        if (showQuality) {
            ControlsButton(
                icon = Icons.Default.HighQuality,
                onClick = onQualityClick,
                onLongClick = onQualityClick,
                verticalSpacing = MaterialTheme.padding.small,
            )
        }

        if (isPipAvailable) {
            ControlsButton(
                Icons.Default.PictureInPictureAlt,
                onClick = onPipClick,
                verticalSpacing = MaterialTheme.padding.small,
            )
        }

        ControlsButton(
            Icons.Default.AspectRatio,
            onClick = onAspectClick,
            verticalSpacing = MaterialTheme.padding.small,
        )
    }
}
