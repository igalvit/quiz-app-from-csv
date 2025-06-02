package es.igalvit.quizappfromcsv.data

import android.content.ContentResolver
import android.net.Uri
import es.igalvit.quizappfromcsv.QuizQuestion

/**
 * Test implementation of [QuestionRepository] that provides predefined questions.
 * Used for testing purposes to avoid dependencies on external CSV files.
 */
class TestQuestionRepository : QuestionRepository {
    /** Mock error message to be used in testing */
    private var mockError: String? = null

    /** Predefined list of test questions */
    private var mockQuestions = listOf(
        QuizQuestion(
            questionText = "Test Question 1",
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctAnswer = "A",
            group = "1-50"
        ),
        QuizQuestion(
            questionText = "Test Question 2",
            options = listOf("Option A", "Option B", "Option C", "Option D"),
            correctAnswer = "B",
            group = "51-100"
        )
    )

    /**
     * Sets a mock error message for testing error scenarios
     */
    fun setMockError(error: String?) {
        mockError = error
    }

    /**
     * Sets custom questions for testing
     */
    fun setMockQuestions(newQuestions: List<QuizQuestion>) {
        mockQuestions = newQuestions
    }

    /**
     * Gets the predefined list of test questions.
     * @return List of predefined quiz questions for testing
     */
    override fun getQuestions(): List<QuizQuestion> = mockQuestions

    /**
     * Mock implementation that can simulate errors and return predefined questions.
     * @param contentResolver Not used in test implementation
     * @param uri Not used in test implementation
     * @return List of predefined quiz questions for testing
     * @throws IllegalArgumentException when mockError is set
     */
    override fun loadQuestionsFromCsv(contentResolver: ContentResolver, uri: Uri): List<QuizQuestion> {
        mockError?.let { error ->
            throw IllegalArgumentException(error)
        }
        return mockQuestions
    }
}
