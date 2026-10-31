package eu.kanade.tachiyomi.ui.player

import android.content.Context
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Gate A only: validates an installed debug APK and Android instrumentation on
 * an x86_64 API 34 emulator. Does not start PlayerActivity or claim MPV startup.
 */
@RunWith(AndroidJUnit4::class)
class EmulatorInfrastructureGateTest {
    @Test
    fun instrumentationRunsOnApi34X8664WithInstalledLalapo() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context: Context = instrumentation.targetContext
        assertEquals("com.noirero.lalapo.dev", context.packageName)
        assertEquals(34, Build.VERSION.SDK_INT)
        assertTrue("Unexpected ABIs: ${Build.SUPPORTED_ABIS.joinToString()}", Build.SUPPORTED_ABIS.contains("x86_64"))
        assertNotNull(context.packageManager.getApplicationInfo(context.packageName, 0))
    }
}
