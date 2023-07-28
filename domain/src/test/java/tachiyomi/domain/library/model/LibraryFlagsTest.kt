package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import tachiyomi.domain.library.anime.model.AnimeLibrarySort

@Execution(ExecutionMode.CONCURRENT)
class LibraryFlagsTest {

    @Test
    fun `Check the amount of flags`() {
        LibraryDisplayMode.values.size shouldBe 4
        AnimeLibrarySort.types.size shouldBe 8
        AnimeLibrarySort.directions.size shouldBe 2
    }

    @Test
    fun `Test Flag plus operator (LibraryDisplayMode)`() {
        val current = LibraryDisplayMode.List
        val new = LibraryDisplayMode.CoverOnlyGrid
        val flag = current + new

        flag shouldBe 0b00000011
    }

    @Test
    fun `Test Flag plus operator (LibrarySort)`() {
        val animecurrent = AnimeLibrarySort(AnimeLibrarySort.Type.LastSeen, AnimeLibrarySort.Direction.Ascending)
        val newanime = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val animeflag = animecurrent + newanime
        animeflag shouldBe 0b01011100
    }

    @Test
    fun `Test Flag plus operator`() {
        val display = LibraryDisplayMode.CoverOnlyGrid
        val animesort = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val animeflag = display + animesort
        animeflag shouldBe 0b01011111
    }

    @Test
    fun `Test Flag plus operator with old flag as base`() {
        val currentDisplay = LibraryDisplayMode.List
        val currentanimeSort = AnimeLibrarySort(AnimeLibrarySort.Type.UnseenCount, AnimeLibrarySort.Direction.Descending)
        val currentanimeFlag = currentDisplay + currentanimeSort

        val display = LibraryDisplayMode.CoverOnlyGrid
        val animesort = AnimeLibrarySort(AnimeLibrarySort.Type.DateAdded, AnimeLibrarySort.Direction.Ascending)
        val animeflag = currentanimeFlag + display + animesort

        currentanimeFlag shouldBe 0b00001110
        animeflag shouldBe 0b01011111
        animeflag shouldNotBe currentanimeFlag
    }

    @Test
    fun `Test default flags`() {
        val animesort = AnimeLibrarySort.default
        val display = LibraryDisplayMode.default
        val animeflag = display + animesort.type + animesort.direction

        animeflag shouldBe 0b01000000
    }
}
