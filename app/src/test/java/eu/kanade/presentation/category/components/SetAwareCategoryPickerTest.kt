package eu.kanade.presentation.category.components

import androidx.compose.ui.state.ToggleableState
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class SetAwareCategoryPickerTest {
    @Test
    fun checkedUncheckedAndMixedMembership() {
        val counts = mapOf(10L to 2, 20L to 1)
        assertEquals(ToggleableState.On, pickerState(10, counts, 2, emptyMap()))
        assertEquals(ToggleableState.Indeterminate, pickerState(20, counts, 2, emptyMap()))
        assertEquals(ToggleableState.Off, pickerState(30, counts, 2, emptyMap()))
    }

    @Test
    fun clickingMixedSelectsAllAndCheckedClearsAll() {
        val mixed = toggleCategoryPending(longArrayOf(), 20L, ToggleableState.Indeterminate)
        assertArrayEquals(longArrayOf(20L), mixed)
        assertEquals(ToggleableState.On, pickerState(20L, mapOf(20L to 1), 2, mapOf(20L to true)))
        val unchecked = toggleCategoryPending(mixed, 20L, ToggleableState.On)
        assertArrayEquals(longArrayOf(-20L), unchecked)
    }

    @Test
    fun switchingSetsDoesNotDiscardOtherPendingMembership() {
        val first = toggleCategoryPending(longArrayOf(), 101L, ToggleableState.Off)
        val second = toggleCategoryPending(first, 202L, ToggleableState.Indeterminate)
        assertArrayEquals(longArrayOf(101L, 202L), second)
        assertEquals(ToggleableState.On, pickerState(101L, emptyMap(), 2, mapOf(101L to true)))
    }

    @Test
    fun removedCategoryIsPrunedButMovedCategoryIdRemainsValid() {
        val before = longArrayOf(101L, -202L, 303L)
        assertArrayEquals(longArrayOf(101L, 303L), retainValidCategoryPending(before, setOf(101L, 303L)))
        assertArrayEquals(before, retainValidCategoryPending(before, setOf(101L, 202L, 303L)))
    }

    @Test
    fun adaptiveCapacityHandlesEightyAndFifteenCategories() {
        assertEquals(20, pickerPageCapacity(520, 80))
        assertEquals(20, pickerPageCapacity(520, 15))
        assertEquals(10, pickerPageCapacity(520, 10))
        assertEquals(2, pickerPageCapacity(100, 80))
        assertEquals(1, pickerPageCapacity(100, 8))
    }

    @Test
    fun twoColumnOrderIsColumnMajor() {
        val (left, right) = pickerColumnMajor((1..20).toList())
        assertEquals((1..10).toList(), left)
        assertEquals((11..20).toList(), right)
        val (oddLeft, oddRight) = pickerColumnMajor((1..15).toList())
        assertEquals((1..8).toList(), oddLeft)
        assertEquals((9..15).toList(), oddRight)
    }

    @Test
    fun savedPrimitivePendingCanBeRestoredByCategoryId() {
        val saved = longArrayOf(44L, -55L)
        val restored = saved.associate { kotlin.math.abs(it) to (it > 0) }
        assertEquals(ToggleableState.On, pickerState(44L, emptyMap(), 1, restored))
        assertEquals(ToggleableState.Off, pickerState(55L, mapOf(55L to 1), 1, restored))
    }
}
