package eu.kanade.presentation.library.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.ui.unit.Density
import eu.kanade.presentation.theme.ModernLibraryGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ModernLibraryGridGeometryTest {
    private fun tracks(width: Int, columns: Int, density: Float = 1f): List<Int> = with(libraryGridCells(columns)) {
        with(Density(density)) {
            val available = (width - 2 * ModernLibraryGeometry.GridPadding.roundToPx()).coerceAtLeast(0)
            calculateCrossAxisCellSizes(available, ModernLibraryGeometry.GridSpacing.roundToPx())
        }
    }

    @Test
    fun `canonical fixed grid has 18dp cover edges and 8dp gutters without losing pixels`() {
        val sizes = tracks(432, 3)
        assertEquals(listOf(135, 135, 134), sizes)
        assertEquals(432, sizes.sum() + 28)
        assertEquals(18f, ModernLibraryGeometry.GridPadding.value + ModernLibraryGeometry.GridItemInset.value)
        assertEquals(8f, ModernLibraryGeometry.GridItemInset.value * 2 + ModernLibraryGeometry.GridSpacing.value)
        val covers = sizes.map { it - 8 }
        assertEquals(listOf(127, 127, 126), covers)
        assertEquals(432, covers.sum() + 2 * 18 + 2 * 8)
    }

    @Test
    fun `auto keeps the existing adaptive minimum around container breakpoints`() {
        assertTrue(libraryGridCells(0) is GridCells.Adaptive)
        assertEquals(1, tracks(283, 0).size)
        assertEquals(2, tracks(284, 0).size)
        assertEquals(2, tracks(411, 0).size)
        assertEquals(3, tracks(412, 0).size)
        assertEquals(2, tracks(320, 0).size)
        assertEquals(3, tracks(432, 0).size)
    }

    @Test
    fun `fixed preferences survive narrow transient and higher density containers`() {
        for (columns in 1..6) {
            assertTrue(libraryGridCells(columns) is GridCells.Fixed)
            for (width in listOf(0, 27, 28, 29, 320, 432, 840)) {
                val sizes = tracks(width, columns)
                assertEquals(columns, sizes.size)
                assertTrue(sizes.all { it >= 0 })
                assertEquals((width - 28).coerceAtLeast(0), sizes.sum())
            }
        }
        assertEquals(3, tracks(864, 0, density = 2f).size)
        assertEquals(2, tracks(640, 0, density = 2f).size)
        assertEquals(1, tracks(0, 0).size)
    }
}
