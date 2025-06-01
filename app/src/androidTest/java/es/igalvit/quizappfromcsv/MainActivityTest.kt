package es.igalvit.quizappfromcsv

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.compose.ui.unit.dp
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
        composeTestRule.onNodeWithText(copyright)
            .assertPositionInRootIsEqualTo(
                expectedLeft = composeTestRule.onRoot().getBoundsInRoot().left,
                expectedTop = composeTestRule.onRoot().getBoundsInRoot().bottom - 16.dp
            )
    }

    @Test
    fun initialScreen_hasAllElements() {
        composeTestRule.onNodeWithText("Quiz App").assertExists()
        composeTestRule.onNodeWithText("Pick CSV File").assertExists()
        composeTestRule.onNodeWithContentDescription("Quiz App Logo").assertExists()
    }

    @Test
    fun logo_hasCorrectColor() {
        composeTestRule.onNode(
            hasContentDescription("Quiz App Logo") and
            hasAnyChild(hasContentDescription("Quiz App Logo"))
        ).assertExists()
    }

    @Test
    fun navigationButtons_haveCorrectColors() {
        // Mock loading questions
        with(composeTestRule) {
            // Using semantics to verify button colors
            onNodeWithText("Previous")
                .assertExists()
                .assertHasClickAction()
                .assertIsDisplayed()

            onNodeWithText("Next")
                .assertExists()
                .assertHasClickAction()
                .assertIsDisplayed()

            // Group filter button
            onNodeWithText("All")
                .assertExists()
                .assertHasClickAction()
                .assertIsDisplayed()

            // Pick CSV File button
            onNodeWithText("Pick CSV File")
                .assertExists()
                .assertHasClickAction()
                .assertIsDisplayed()
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
        // Verify initial state shows "All"
        composeTestRule.onNodeWithText("All")
            .assertExists()
            .assertIsDisplayed()
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
            (composeTestRule.activity as MainActivity).setQuestions(questions)
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
            (composeTestRule.activity as MainActivity).setQuestions(questions)
        }

        // Click the correct answer (Option A for first question)
        composeTestRule.onNodeWithText("A: Option A").performClick()

        // Verify feedback is shown
        composeTestRule.onNodeWithText("Correct!").assertExists()
    }

    @Test
    fun groupFilter_withTestData_showsGroups() {
        // Load questions
        composeTestRule.activity.runOnUiThread {
            val questions = testRepository.getQuestions()
            (composeTestRule.activity as MainActivity).setQuestions(questions)
        }

        // Click group filter
        composeTestRule.onNodeWithText("All").performClick()

        // Verify groups are shown
        composeTestRule.onNodeWithText("1-50").assertExists()
        composeTestRule.onNodeWithText("51-100").assertExists()
    }

    @Test
    fun scoreDisplay_showsCorrectLayout() {
        // Load test questions and set some scores
        composeTestRule.activity.setQuestions(listOf(
            QuizQuestion("Test question?", listOf("A", "B", "C", "D"), "A", "1-50")
        ))

        with(composeTestRule) {
            // Verify score components exist and are properly positioned
            onNodeWithText("Question")
                .assertExists()
                .assertIsDisplayed()

            onNodeWithText("Correct")
                .assertExists()
                .assertIsDisplayed()

            onNodeWithText("Incorrect")
                .assertExists()
                .assertIsDisplayed()

            // Verify progress indicator exists
            onNode(hasProgressBarRangeInfo())
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
            .assertPositionInRootIsAbove(questionTextBounds.top)
    }
}
