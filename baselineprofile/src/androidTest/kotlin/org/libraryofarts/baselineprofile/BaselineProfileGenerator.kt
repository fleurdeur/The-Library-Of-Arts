package org.libraryofarts.baselineprofile

import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates a baseline profile for the phone app. Run on a physical device:
 *   ./gradlew :baselineprofile:connectedAndroidTest
 * Then copy the generated baseline-prof.txt into app/src/main/.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun startup() = rule.measureRepeated(
        packageName = "org.libraryofarts.app",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.COLD,
    ) {
        pressHome()
        startActivityAndWait()
    }
}
