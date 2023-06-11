package eu.kanade.presentation.more

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.Divider
import eu.kanade.tachiyomi.R

@Composable
fun LogoHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = if (isSystemInDarkTheme()) painterResource(R.drawable.ic_ani) else painterResource(R.drawable.ic_ani_monochrome_launcher),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .padding(horizontal = 32.dp, vertical = 10.dp)
                .size(128.dp),
        )

        Divider()
    }
}
