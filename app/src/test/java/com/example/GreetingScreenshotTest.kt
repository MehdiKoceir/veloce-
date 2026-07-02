package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.components.AthleticBarChart
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun athletic_chart_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true) {
        AthleticBarChart(
          data = listOf(5.0f, 10.0f, 15.0f, 20.0f, 12.0f, 8.0f, 14.0f),
          labels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"),
          metricLabel = "Volume hebdomadaire (km)"
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/athletic_chart.png")
  }
}
