package io.github.ieswar23.buddyup.screenshots

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario

import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.captureRoboImage
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.ieswar23.buddyup.MainActivity
import io.github.ieswar23.buddyup.data.repository.UserRepository
import io.github.ieswar23.buddyup.domain.model.AgeRange
import io.github.ieswar23.buddyup.domain.model.ThemeMode
import io.github.ieswar23.buddyup.domain.model.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import javax.inject.Inject

/**
 * Renders the real app (Hilt graph, Room, DataStore, Retrofit + MockInterceptor) on the JVM and
 * captures README screenshots with Roborazzi.
 *
 * `./gradlew testDebugUnitTest` runs these as smoke tests without writing images;
 * `./gradlew recordRoborazziDebug` (re)writes the PNGs into `docs/screenshots/`.
 */
@OptIn(ExperimentalTestApi::class)
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = HiltTestApplication::class, qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
class AppScreenshotTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var userRepository: UserRepository

    private var scenario: ActivityScenario<MainActivity>? = null

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        scenario?.close()
    }

    @Test
    fun onboarding() {
        launch()
        waitForText("Make a buddy")
        capture("01_onboarding")
    }

    @Test
    fun discover() {
        seedCompletedProfile()
        launch()
        waitForSync()
        capture("02_discover")
    }

    @Test
    fun waves() {
        seedCompletedProfile()
        launch()
        waitForSync()
        openTab("Waves")
        waitForText("Wave back")
        capture("03_waves")
    }

    @Test
    fun friends() {
        seedCompletedProfile()
        launch()
        waitForSync()
        openTab("Friends")
        waitForText("Kavya Iyer")
        capture("04_friends")
    }

    @Test
    fun chatWithKavya() {
        seedCompletedProfile()
        launch()
        waitForSync()
        openTab("Friends")
        waitForText("Kavya Iyer")
        composeRule.onAllNodes(hasText("Kavya Iyer") and hasClickAction()).onFirst().performClick()
        waitForText("Brahmin's")
        capture("05_chat")
    }

    @Test
    fun meetups() {
        seedCompletedProfile()
        launch()
        waitForSync()
        openTab("Meetups")
        waitForText(" going · ")
        capture("06_meetups")
    }

    @Test
    fun profileDark() {
        seedCompletedProfile(theme = ThemeMode.DARK)
        launch()
        waitForSync()
        openTab("Profile")
        waitForText("Appearance")
        capture("07_profile_dark")
    }

    private fun seedCompletedProfile(theme: ThemeMode = ThemeMode.LIGHT) = runBlocking {
        userRepository.saveProfile(
            UserProfile(
                name = "Aditi Kumar",
                ageRange = AgeRange.AGE_25_34,
                city = "Bengaluru",
                bio = "Product designer, new to Bengaluru. Weekend trekker, filter-coffee loyalist and board-game host.",
                interests = listOf("Hiking", "Coffee", "Board games", "Reading", "Photography", "Cooking"),
            )
        )
        userRepository.setThemeMode(theme)
        userRepository.completeOnboarding()
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        composeRule.waitForIdle()
    }

    /** The first launch syncs from the mock API (300–700 ms latency) into Room; wait for real cards. */
    private fun waitForSync() {
        waitForText("buddies to meet")
        waitForText("shared interest")
    }

    private fun openTab(label: String) {
        composeRule.onAllNodes(hasText(label) and hasClickAction()).onFirst().performClick()
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodes(hasText(text, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun capture(name: String) {
        // Let entrance animations settle before taking the picture.
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.waitForIdle()
        composeRule.onRoot().captureRoboImage(
            filePath = "../docs/screenshots/$name.png",
            roborazziOptions = RoborazziOptions(recordOptions = RoborazziOptions.RecordOptions(resizeScale = 0.44)),
        )
    }
}
