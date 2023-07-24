package tachiyomi.presentation.core.components.material

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

// AM (BR) -->
val bottomSuperLargePaddingValues = PaddingValues(bottom = MaterialTheme.padding.superLarge)
// <--AM (BR)

const val ReadItemAlpha = .38f
const val SecondaryItemAlpha = .78f

class Padding {

    // AM (BR) -->
    val superLarge = 72.dp
    // <-- AM (BR)

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val small = 8.dp

    val tiny = 4.dp

    // AM (BR) -->
    val none = 0.dp
    // <-- AM (BR)
}

val MaterialTheme.padding: Padding
    get() = Padding()
