package es.igalvit.quizappfromcsv.data

import android.content.ContentResolver
import android.net.Uri
import es.igalvit.quizappfromcsv.QuizQuestion

/**
 * Test implementation of [QuestionRepository] that provides predefined questions.
 * Used for testing purposes to avoid dependencies on external CSV files.
 */
class TestQuestionRepository : QuestionRepository {
    /** Predefined list of test questions */
    private var questions = listOf(
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
     * Gets the predefined list of test questions.
     * @return List of predefined quiz questions for testing
     */
    override fun getQuestions(): List<QuizQuestion> = questions

    /**
     * Mock implementation that ignores the CSV file and returns predefined questions.
     * @param contentResolver Not used in test implementation
     * @param uri Not used in test implementation
     * @return List of predefined quiz questions for testing
     */
    override fun loadQuestionsFromCsv(contentResolver: ContentResolver, uri: Uri): List<QuizQuestion> = questions
}
