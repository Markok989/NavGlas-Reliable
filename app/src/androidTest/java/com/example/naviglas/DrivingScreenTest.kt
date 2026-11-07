package com.example.naviglas

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class DrivingScreenTest {
    @get:Rule(order = 0) val permissions = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS)
    @get:Rule(order = 1) val compose = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        compose.waitUntil(15000) { compose.onAllNodesWithContentDescription("Pokreni slušanje").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "screenshots").apply { mkdirs() }
        instrumentation.uiAutomation.takeScreenshot()?.let { bitmap ->
            File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun homeAndSettingsPersistRecipient() {
        ready()
        compose.onNodeWithText("Vaš glas. Vaš put.").assertIsDisplayed()
        screenshot("home-portrait")
        compose.onNodeWithContentDescription("Podešavanja").performClick()
        compose.onNodeWithText("SOS primalac: broj ili puno ime").performScrollTo().performTextReplacement("+381641234567")
        compose.onNodeWithText("Sačuvaj").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("+381641234567", compose.activity.getSharedPreferences("NaviGlasPrefs", Context.MODE_PRIVATE).getString("SOS_NUMBER", null))
        }
        compose.onNodeWithContentDescription("Podešavanja").performClick()
        compose.onNodeWithText("SOS primalac: broj ili puno ime").performScrollTo().assertTextContains("+381641234567")
        screenshot("settings")
    }
    @Test fun landscapeKeepsEssentialControls() {
        ready()
        compose.runOnIdle { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
        ready()
        compose.onNodeWithContentDescription("Podešavanja").assertExists()
        compose.onNodeWithContentDescription("Pokreni slušanje").assertExists()
        screenshot("home-landscape")
    }
    @Test fun quickReplyWithoutSenderDoesNotSend() {
        ready()
        compose.onNodeWithText("Brzi odgovor").performScrollTo().performClick()
        compose.onNodeWithText("Nema poslednjeg pošiljaoca. Recite: pošalji poruku i ime kontakta.").assertExists()
    }
}

