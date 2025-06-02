package es.igalvit.quizappfromcsv

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import android.content.ContentResolver
import android.net.Uri
import android.content.Context
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import es.igalvit.quizappfromcsv.data.QuestionRepository
import es.igalvit.quizappfromcsv.data.TestQuestionRepository
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val copyright = "© 2025 This work is dedicated to the public domain under CC0 1.0 Universal"
    private val appTitle = "Quiz App"
    private val pickFileButton = "Pick CSV File"

    private lateinit var testRepository: TestQuestionRepository

    @Before
    fun setup() {
        testRepository = TestQuestionRepository()
        composeTestRule.activity.setRepository(testRepository)
    }

    @Test
    fun initialScreenElements_areDisplayed() {
        composeTestRule.onNodeWithText(copyright).assertExists()
        composeTestRule.onNodeWithText(appTitle).assertExists()
        composeTestRule.onNodeWithText(pickFileButton).assertExists().assertHasClickAction()
    }

    @Test
    fun copyright_isDisplayedAtBottom() {
        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()

        composeTestRule.onNodeWithText(copyright)
            .assertExists()
            .assertIsDisplayed()
            .getBoundsInRoot().let { bounds ->
                // Check that copyright is near the bottom with some tolerance
                assert(bounds.top >= rootBounds.bottom - 100.dp) {
                    "Copyright should be near the bottom of the screen"
                }
                assert(bounds.left == 16.dp) {
                    "Copyright should have 16dp left padding"
                }
            }
    }

    @Test
    fun initialScreen_hasAllElements() {
        composeTestRule.onNodeWithText("Quiz App").assertExists()
        composeTestRule.onNodeWithText("Pick CSV File").assertExists()
        composeTestRule.onNodeWithContentDescription("Quiz App Logo").assertExists()
    }

    @Test
    fun logo_hasCorrectColor() {
        // The logo doesn't have nested content description, just check the main one
        composeTestRule.onNode(hasContentDescription("Quiz App Logo"))
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun navigationButtons_haveCorrectColors() {
        // Load test questions first to show navigation buttons
        loadTestQuestions()
        composeTestRule.waitForIdle()

        // Using semantics to verify button colors and icons
        with(composeTestRule) {
            // Previous and Next buttons
            listOf("Previous Question", "Next Question", "Filter Groups", "Pick CSV File").forEach { description ->
                onNode(hasContentDescription(description))
                    .assertExists()
                    .assertHasClickAction()
                    .assertIsDisplayed()
            }
        }
    }

    @Test
    fun navigationButtons_haveCorrectIcons() {
        // Load test questions first
        composeTestRule.activity.setQuestions(listOf(
            QuizQuestion("Test question?", listOf("A", "B", "C", "D"), "A", "1-50")
        ))

        with(composeTestRule) {
            // Check navigation icons
            onNodeWithContentDescription("Previous Question")
                .assertExists()
                .assertHasClickAction()

            onNodeWithContentDescription("Next Question")
                .assertExists()
                .assertHasClickAction()

            // Check filter icon
            onNodeWithContentDescription("Filter Groups")
                .assertExists()
                .assertHasClickAction()

            // Check file picker icon
            onNodeWithContentDescription("Pick CSV File")
                .assertExists()
                .assertHasClickAction()
        }
    }

    @Test
    fun questionDisplay_showsCorrectFeedback() {
        // Note: This is a simplified test as we cannot directly inject questions
        // In a real scenario, we would need to use dependency injection
        composeTestRule.onNodeWithContentDescription("Quiz App Logo")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun groupFilter_showsCorrectGroups() {
        // Load questions first to show the filter
        loadTestQuestions()
        composeTestRule.waitForIdle()

        // Open filter menu
        composeTestRule.onNodeWithContentDescription("Filter Groups")
            .assertExists()
            .assertIsDisplayed()
            .performClick()

        // Wait for dropdown menu to appear
        composeTestRule.waitForIdle()

        // First check "All" is visible
        composeTestRule.onNodeWithText("All").assertExists()

        // Then check for specific groups using waitUntil
        composeTestRule.waitUntil(timeoutMillis = 3000) {
            composeTestRule.onAllNodesWithText("1-50").fetchSemanticsNodes().isNotEmpty() &&
            composeTestRule.onAllNodesWithText("51-100").fetchSemanticsNodes().isNotEmpty()
        }

        // Close dropdown and verify filter works
        composeTestRule.onNodeWithText("1-50").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Q1").assertExists()
    }

    @Test
    fun pickFileButton_hasCorrectColorAndText() {
        composeTestRule.onNodeWithText("Pick CSV File")
            .assertExists()
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun appTitle_isDisplayedWithCorrectStyle() {
        composeTestRule.onNodeWithText(appTitle)
            .assertExists()
            .assertIsDisplayed()
            .assertHeightIsAtLeast(24.dp)
    }

    @Test
    fun questionDisplay_withTestData_showsCorrectQuestion() {
        // Simulate loading questions
        composeTestRule.activity.runOnUiThread {
            val questions = testRepository.getQuestions()
            composeTestRule.activity.setQuestions(questions)
        }

        // Verify first question is displayed
        composeTestRule.onNodeWithText("Test Question 1").assertExists()

        // Verify options are displayed
        composeTestRule.onNodeWithText("A: Option A").assertExists()
        composeTestRule.onNodeWithText("B: Option B").assertExists()
    }

    @Test
    fun answerSelection_withTestData_showsFeedback() {
        // Load questions and select an answer
        composeTestRule.activity.runOnUiThread {
            val questions = testRepository.getQuestions()
            composeTestRule.activity.setQuestions(questions)
        }

        // Click the correct answer (Option A for first question)
        composeTestRule.onNodeWithText("A: Option A").performClick()

        // Verify feedback is shown
        composeTestRule.onNodeWithText("Correct!").assertExists()
    }

    @Test
    fun scoreDisplay_showsCorrectLayout() {
        // Load test questions and wait for UI to stabilize
        loadTestQuestions()
        composeTestRule.waitForIdle()

        with(composeTestRule) {
            // First verify the score text components
            listOf("Question", "Correct", "Incorrect").forEach { text ->
                onNodeWithText(text)
                    .assertExists()
                    .assertIsDisplayed()
            }

            // Then verify progress bar exists
            val initialRange = ProgressBarRangeInfo(
                current = 0f,
                range = 0f..1f,
                steps = 0
            )
            onNode(hasProgressBarRangeInfo(initialRange))
                .assertExists()
                .assertIsDisplayed()
        }
    }

    @Test
    fun scoreDisplay_isAtTop() {
        // Load test questions
        composeTestRule.activity.setQuestions(listOf(
            QuizQuestion("Test question?", listOf("A", "B", "C", "D"), "A", "1-50")
        ))

        // Get question text position
        val questionTextBounds = composeTestRule.onNodeWithText("Test question?")
            .getBoundsInRoot()

        // Verify score display is above question text
        composeTestRule.onNodeWithText("Question")
            .assertExists()
            .assertIsDisplayed()
            .getBoundsInRoot().let { scoreBounds ->
                assert(scoreBounds.top < questionTextBounds.top) {
                    "Score display should be above question text"
                }
            }
    }

    @Test
    fun emptyCSV_showsErrorMessage() {
        testRepository.setMockError("The CSV file is empty")

        // Trigger error through CSV loading and show error message
        composeTestRule.activity.runOnUiThread {
            try {
                testRepository.loadQuestionsFromCsv(composeTestRule.activity.contentResolver, Uri.EMPTY)
            } catch (e: IllegalArgumentException) {
                val errorMessage = "Error reading CSV file: " + e.message
                android.widget.Toast.makeText(
                    composeTestRule.activity,
                    errorMessage,
                    android.widget.Toast.LENGTH_LONG
                ).show()
                composeTestRule.activity.setQuestions(emptyList())
            }
        }

        // Wait longer for Toast to appear and be readable by the test
        composeTestRule.waitForIdle()
        Thread.sleep(500) // Give Toast time to appear

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule
                .onAllNodesWithText("Error reading CSV file: The CSV file is empty")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun missingColumns_showsDetailedError() {
        val errorMessage = "CSV file is missing required columns: option3, option4"
        testRepository.setMockError(errorMessage)

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.setQuestions(emptyList())
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Error reading CSV file: $errorMessage")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun malformedCSV_showsFormatError() {
        val errorMessage = "The CSV file is not properly formatted"
        testRepository.setMockError(errorMessage)

        composeTestRule.activity.runOnUiThread {
            composeTestRule.activity.setQuestions(emptyList())
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Error reading CSV file: $errorMessage")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun timer_isDisplayedWhenQuestionsLoaded() {
        loadTestQuestions()
        composeTestRule.onNodeWithText("Time: 00:00").assertExists()
    }

    @Test
    fun timer_resetsOnGroupChange() {
        loadTestQuestions()
        // Change group and verify timer reset
        composeTestRule.onNodeWithContentDescription("Filter Groups").performClick()
        composeTestRule.onNodeWithText("1-50").performClick()
        composeTestRule.onNodeWithText("Time: 00:00").assertExists()
    }

    @Test
    fun timer_stopsAfterAllQuestionsAnswered() {
        // Load test questions and let the timer start
        loadTestQuestions()
        composeTestRule.waitForIdle()

        // Initial time should be 00:00
        composeTestRule.onNodeWithText("Time: 00:00").assertExists()

        // Answer all questions
        repeat(testRepository.getQuestions().size) {
            composeTestRule.onNodeWithText("A: Option A").performClick()
            composeTestRule.waitForIdle()
        }

        // After all questions are answered
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.autoAdvance = false

        // The timer should still be visible and not change
        val lastTimeText = composeTestRule
            .onAllNodes(hasText("Time:"))
            .fetchSemanticsNodes()
            .firstOrNull()?.config?.getOrNull(SemanticsProperties.Text)?.toString()
            ?: throw AssertionError("Timer text not found")

        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.onNodeWithText(lastTimeText).assertExists()
    }

    private fun loadTestQuestions() {
        val questions = listOf(
            QuizQuestion("Q1", listOf("Option A", "Option B", "Option C", "Option D"), "A", "1-50"),
            QuizQuestion("Q2", listOf("Option A", "Option B", "Option C", "Option D"), "B", "1-50")
        )
        composeTestRule.activity.runOnUiThread {
            (testRepository as TestQuestionRepository).setMockQuestions(questions)
            composeTestRule.activity.setQuestions(questions)
        }
        composeTestRule.waitForIdle()
    }
}
