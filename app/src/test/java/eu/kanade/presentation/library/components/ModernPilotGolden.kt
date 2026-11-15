package eu.kanade.presentation.library.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import java.io.File

/** Native Robolectric component goldens, independent of emulator/APK screenshots. */
internal fun assertModernPilotGolden(name: String, node: SemanticsNodeInteraction) {
    val actual = node.captureToImage().asAndroidBitmap()
    val expectedFile = File("src/test/snapshots/modern-pilot/$name.png")
    if (System.getProperty("modernPilot.recordScreenshots").toBoolean()) {
        expectedFile.parentFile.mkdirs()
        expectedFile.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return
    }
    assertTrue("Missing golden $expectedFile; record and review the fixture explicitly", expectedFile.isFile)
    val expected = requireNotNull(BitmapFactory.decodeFile(expectedFile.path))
    assertEquals("Golden width", expected.width, actual.width)
    assertEquals("Golden height", expected.height, actual.height)
    val expectedPixels = IntArray(expected.width * expected.height)
    val actualPixels = IntArray(actual.width * actual.height)
    expected.getPixels(expectedPixels, 0, expected.width, 0, 0, expected.width, expected.height)
    actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)
    val changed = expectedPixels.indices.count { expectedPixels[it] != actualPixels[it] }
    if (changed > 0) {
        val report = File("build/reports/modern-pilot/$name.actual.png")
        report.parentFile.mkdirs()
        report.outputStream().use { actual.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    assertEquals("Changed pixels in $name; actual is in build/reports/modern-pilot", 0, changed)
}
