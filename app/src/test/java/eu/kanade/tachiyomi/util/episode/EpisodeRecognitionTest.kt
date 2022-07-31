package eu.kanade.tachiyomi.util.episode

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class EpisodeRecognitionTest {

    @Test
    fun `Basic Ch prefix`() {
        val animeTitle = "Mokushiroku Alice"

        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch.4: Misrepresentation", 4f)
    }

    @Test
    fun `Basic Ch prefix with space after period`() {
        val animeTitle = "Mokushiroku Alice"

        assertEpisode(animeTitle, "Mokushiroku Alice Vol. 1 Ch. 4: Misrepresentation", 4f)
    }

    @Test
    fun `Basic Ch prefix with decimal`() {
        val animeTitle = "Mokushiroku Alice"

        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch.4.1: Misrepresentation", 4.1f)
        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch.4.4: Misrepresentation", 4.4f)
    }

    @Test
    fun `Basic Ch prefix with alpha postfix`() {
        val animeTitle = "Mokushiroku Alice"

        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch.4.a: Misrepresentation", 4.1f)
        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch.4.b: Misrepresentation", 4.2f)
        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch.4.extra: Misrepresentation", 4.99f)
    }

    @Test
    fun `Name containing one number`() {
        val animeTitle = "Bleach"

        assertEpisode(animeTitle, "Bleach 567 Down With Snowwhite", 567f)
    }

    @Test
    fun `Name containing one number and decimal`() {
        val animeTitle = "Bleach"

        assertEpisode(animeTitle, "Bleach 567.1 Down With Snowwhite", 567.1f)
        assertEpisode(animeTitle, "Bleach 567.4 Down With Snowwhite", 567.4f)
    }

    @Test
    fun `Name containing one number and alpha`() {
        val animeTitle = "Bleach"

        assertEpisode(animeTitle, "Bleach 567.a Down With Snowwhite", 567.1f)
        assertEpisode(animeTitle, "Bleach 567.b Down With Snowwhite", 567.2f)
        assertEpisode(animeTitle, "Bleach 567.extra Down With Snowwhite", 567.99f)
    }

    @Test
    fun `Episode containing anime title and number`() {
        val animeTitle = "Solanin"

        assertEpisode(animeTitle, "Solanin 028 Vol. 2", 28f)
    }

    @Test
    fun `Episode containing anime title and number decimal`() {
        val animeTitle = "Solanin"

        assertEpisode(animeTitle, "Solanin 028.1 Vol. 2", 28.1f)
        assertEpisode(animeTitle, "Solanin 028.4 Vol. 2", 28.4f)
    }

    @Test
    fun `Episode containing anime title and number alpha`() {
        val animeTitle = "Solanin"

        assertEpisode(animeTitle, "Solanin 028.a Vol. 2", 28.1f)
        assertEpisode(animeTitle, "Solanin 028.b Vol. 2", 28.2f)
        assertEpisode(animeTitle, "Solanin 028.extra Vol. 2", 28.99f)
    }

    @Test
    fun `Extreme case`() {
        val animeTitle = "Onepunch-Man"

        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 028", 28f)
    }

    @Test
    fun `Extreme case with decimal`() {
        val animeTitle = "Onepunch-Man"

        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 028.1", 28.1f)
        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 028.4", 28.4f)
    }

    @Test
    fun `Extreme case with alpha`() {
        val animeTitle = "Onepunch-Man"

        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 028.a", 28.1f)
        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 028.b", 28.2f)
        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 028.extra", 28.99f)
    }

    @Test
    fun `Episode containing dot v2`() {
        val animeTitle = "random"

        assertEpisode(animeTitle, "Vol.1 Ch.5v.2: Alones", 5f)
    }

    @Test
    fun `Number in anime title`() {
        val animeTitle = "Ayame 14"

        assertEpisode(animeTitle, "Ayame 14 1 - The summer of 14", 1f)
    }

    @Test
    fun `Space between ch x`() {
        val animeTitle = "Mokushiroku Alice"

        assertEpisode(animeTitle, "Mokushiroku Alice Vol.1 Ch. 4: Misrepresentation", 4f)
    }

    @Test
    fun `Episode title with ch substring`() {
        val animeTitle = "Ayame 14"

        assertEpisode(animeTitle, "Vol.1 Ch.1: March 25 (First Day Cohabiting)", 1f)
    }

    @Test
    fun `Episode containing multiple zeros`() {
        val animeTitle = "random"

        assertEpisode(animeTitle, "Vol.001 Ch.003: Kaguya Doesn't Know Much", 3f)
    }

    @Test
    fun `Episode with version before number`() {
        val animeTitle = "Onepunch-Man"

        assertEpisode(animeTitle, "Onepunch-Man Punch Ver002 086 : Creeping Darkness [3]", 86f)
    }

    @Test
    fun `Version attached to episode number`() {
        val animeTitle = "Ansatsu Kyoushitsu"

        assertEpisode(animeTitle, "Ansatsu Kyoushitsu 011v002: Assembly Time", 11f)
    }

    /**
     * Case where the episode title contains the episode
     * But wait it's not actual the episode number.
     */
    @Test
    fun `Number after anime title with episode in episode title case`() {
        val animeTitle = "Tokyo ESP"

        assertEpisode(animeTitle, "Tokyo ESP 027: Part 002: Episode 001", 027f)
    }

    @Test
    fun `Unparseable episode`() {
        val animeTitle = "random"

        assertEpisode(animeTitle, "Foo", -1f)
    }

    @Test
    fun `Episode with time in title`() {
        val animeTitle = "random"

        assertEpisode(animeTitle, "Fairy Tail 404: 00:00", 404f)
    }

    @Test
    fun `Episode with alpha without dot`() {
        val animeTitle = "random"

        assertEpisode(animeTitle, "Asu No Yoichi 19a", 19.1f)
    }

    @Test
    fun `Episode title containing extra and vol`() {
        val animeTitle = "Fairy Tail"

        assertEpisode(animeTitle, "Fairy Tail 404.extravol002", 404.99f)
        assertEpisode(animeTitle, "Fairy Tail 404 extravol002", 404.99f)
        assertEpisode(animeTitle, "Fairy Tail 404.evol002", 404.5f)
    }

    @Test
    fun `Episode title containing omake (japanese extra) and vol`() {
        val animeTitle = "Fairy Tail"

        assertEpisode(animeTitle, "Fairy Tail 404.omakevol002", 404.98f)
        assertEpisode(animeTitle, "Fairy Tail 404 omakevol002", 404.98f)
        assertEpisode(animeTitle, "Fairy Tail 404.ovol002", 404.15f)
    }

    @Test
    fun `Episode title containing special and vol`() {
        val animeTitle = "Fairy Tail"

        assertEpisode(animeTitle, "Fairy Tail 404.specialvol002", 404.97f)
        assertEpisode(animeTitle, "Fairy Tail 404 specialvol002", 404.97f)
        assertEpisode(animeTitle, "Fairy Tail 404.svol002", 404.19f)
    }

    @Test
    fun `Episode title containing commas`() {
        val animeTitle = "One Piece"

        assertEpisode(animeTitle, "One Piece 300,a", 300.1f)
        assertEpisode(animeTitle, "One Piece Ch,123,extra", 123.99f)
        assertEpisode(animeTitle, "One Piece the sunny, goes swimming 024,005", 24.005f)
    }

    @Test
    fun `Episode title containing hyphens`() {
        val animeTitle = "Solo Leveling"

        assertEpisode(animeTitle, "ch 122-a", 122.1f)
        assertEpisode(animeTitle, "Solo Leveling Ch.123-extra", 123.99f)
        assertEpisode(animeTitle, "Solo Leveling, 024-005", 24.005f)
        assertEpisode(animeTitle, "Ch.191-200 Read Online", 191.200f)
    }

    @Test
    fun `Episodes containing season`() {
        assertEpisode("D.I.C.E", "D.I.C.E[Season 001] Ep. 007", 7f)
    }

    @Test
    fun `Episodes in format sx - episode xx`() {
        assertEpisode("The Gamer", "S3 - Episode 20", 20f)
    }

    @Test
    fun `Episodes ending with s`() {
        assertEpisode("One Outs", "One Outs 001", 1f)
    }

    private fun assertEpisode(animeTitle: String, name: String, expected: Float) {
        val episodeNumber = EpisodeRecognition.parseEpisodeNumber(animeTitle, name)
        assertEquals(episodeNumber, expected)
    }
}
