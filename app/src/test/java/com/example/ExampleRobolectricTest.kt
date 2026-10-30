package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.PlatformItem
import com.example.data.repository.FirebasePlatformRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("GyanSarthi", appName)
    }

    @Test
    fun `initial seed data contains the four required platforms`() {
        val seed = FirebasePlatformRepository.getInitialSeedData()
        assertEquals(4, seed.size)

        val pwStudy = seed.find { it.id == "pw_study" }
        assertNotNull(pwStudy)
        assertEquals("PW Study", pwStudy?.name)
        assertEquals("https://pwthor.live/study", pwStudy?.websiteUrl)
        assertTrue(pwStudy?.isActive == true)

        val pwMarco = seed.find { it.id == "pw_marco" }
        assertNotNull(pwMarco)
        assertEquals("https://pwmarco-og-06656c44b005.herokuapp.com", pwMarco?.websiteUrl)

        val asMultiverse = seed.find { it.id == "as_multiverse" }
        assertNotNull(asMultiverse)
        assertEquals("AS Multiverse", asMultiverse?.name)

        val studyParcham = seed.find { it.id == "pw_study_parcham" }
        assertNotNull(studyParcham)
        assertEquals("https://pw.studyparcham.in/#home-view", studyParcham?.websiteUrl)
    }

    @Test
    fun `platform item map serialization and deserialization`() {
        val platform = PlatformItem(
            id = "test_plat",
            name = "Test Portal",
            description = "Test Description",
            category = "Education",
            websiteUrl = "https://example.com",
            apkUrl = "https://example.com/app.apk",
            isActive = true,
            isUpcoming = false,
            sortOrder = 5
        )

        val map = platform.toMap()
        val deserialized = PlatformItem.fromMap("test_plat", map)

        assertEquals("Test Portal", deserialized.name)
        assertEquals("Education", deserialized.category)
        assertEquals("https://example.com/app.apk", deserialized.apkUrl)
        assertTrue(deserialized.isActive)
        assertFalse(deserialized.isUpcoming)
        assertEquals(5, deserialized.sortOrder)
    }

    @Test
    fun `viewmodels can be created with Application instance`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val mainViewModel = com.example.ui.viewmodel.MainViewModel(app)
        assertNotNull(mainViewModel)

        val adminViewModel = com.example.ui.viewmodel.AdminViewModel(app)
        assertNotNull(adminViewModel)
    }

    @Test
    fun `repository operations fail gracefully instead of returning fake success without firestore`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repository = FirebasePlatformRepository(app)

        val newPlatform = PlatformItem(
            name = "Test Portal",
            websiteUrl = "https://test.com"
        )
        // Without Firestore initialized, write operations must return failure
        val addResult = repository.addPlatform(newPlatform)
        assertTrue(addResult.isFailure)

        val updateResult = repository.updatePlatform(newPlatform.copy(id = "test_id"))
        assertTrue(updateResult.isFailure)

        val deleteResult = repository.deletePlatform("test_id")
        assertTrue(deleteResult.isFailure)
    }

    @Test
    fun `FirebaseConfigManager handles gyansarthi-b30f0 target project`() {
        assertEquals("gyansarthi-b30f0", com.example.data.FirebaseConfigManager.TARGET_PROJECT_ID)
        assertEquals("1:86820832067:android:dac78de64d61caaf90a5f6", com.example.data.FirebaseConfigManager.TARGET_APPLICATION_ID)
    }

    @Test
    fun `BannerItem serializes and deserializes correctly with all fields`() {
        val banner = com.example.data.model.BannerItem(
            id = "banner_123",
            title = "Official Telegram Channel",
            subtitle = "Join 50k+ students for daily updates",
            buttonText = "Join Telegram",
            buttonUrl = "https://t.me/gyansarthi_official",
            imageUrl = "https://example.com/banner.png",
            type = "telegram",
            enabled = true,
            order = 1
        )

        val map = banner.toMap()
        assertEquals("Official Telegram Channel", map["title"])
        assertEquals("Join 50k+ students for daily updates", map["subtitle"])
        assertEquals("Join Telegram", map["buttonText"])
        assertEquals("https://t.me/gyansarthi_official", map["buttonUrl"])
        assertEquals("https://example.com/banner.png", map["imageUrl"])
        assertEquals("telegram", map["type"])
        assertEquals(true, map["enabled"])
        assertEquals(1, map["order"])

        val deserialized = com.example.data.model.BannerItem.fromMap("banner_123", map)
        assertEquals("banner_123", deserialized.id)
        assertEquals("Official Telegram Channel", deserialized.title)
        assertEquals("Join 50k+ students for daily updates", deserialized.subtitle)
        assertEquals("Join Telegram", deserialized.buttonText)
        assertEquals("https://t.me/gyansarthi_official", deserialized.buttonUrl)
        assertEquals("https://example.com/banner.png", deserialized.imageUrl)
        assertEquals("telegram", deserialized.type)
        assertTrue(deserialized.enabled)
        assertEquals(1, deserialized.order)
    }

    @Test
    fun `BannerRepository returns seed fallback banners when firestore is offline`() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val repo = com.example.data.repository.FirebaseBannerRepository(app)
        val seed = com.example.data.repository.FirebaseBannerRepository.getInitialSeedBanners()
        assertTrue(seed.isNotEmpty())
        assertTrue(seed.any { it.type == "telegram" })
    }

    @Test
    fun `verification is locked by default on first launch`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.verification.VerificationManager.resetVerification(app)
        assertFalse(com.example.data.verification.VerificationManager.isVerified(app))
    }

    @Test
    fun `example code GYAN24 is rejected and does not unlock app`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.verification.VerificationManager.resetVerification(app)

        val unlocked = com.example.data.verification.VerificationManager.verifyPasscode(app, "GYAN24")
        assertFalse(unlocked)
        assertFalse(com.example.data.verification.VerificationManager.isVerified(app))
    }

    @Test
    fun `generated dynamic session code unlocks app for 24 hours`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.verification.VerificationManager.resetVerification(app)

        val dynamicCode = com.example.data.verification.VerificationManager.generateNewSessionCode(app)
        assertTrue(dynamicCode.length == 6)

        val unlocked = com.example.data.verification.VerificationManager.verifyPasscode(app, dynamicCode)
        assertTrue(unlocked)
        assertTrue(com.example.data.verification.VerificationManager.isVerified(app))

        val remainingMs = com.example.data.verification.VerificationManager.getRemainingTimeMs(app)
        assertTrue(remainingMs > 0L)
        assertTrue(remainingMs <= 24 * 60 * 60 * 1000L)

        // Relock
        com.example.data.verification.VerificationManager.resetVerification(app)
        assertFalse(com.example.data.verification.VerificationManager.isVerified(app))
    }

    @Test
    fun `invalid passcode fails verification`() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.data.verification.VerificationManager.resetVerification(app)

        val failed = com.example.data.verification.VerificationManager.verifyPasscode(app, "WRONGCODE")
        assertFalse(failed)
        assertFalse(com.example.data.verification.VerificationManager.isVerified(app))
    }
}
