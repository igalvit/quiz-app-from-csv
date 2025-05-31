package es.igalvit.quizappfromcsv.data

import android.content.ContentResolver
import android.net.Uri
import es.igalvit.quizappfromcsv.QuizQuestion
import es.igalvit.quizappfromcsv.parseCsvFile

/**
 * Default implementation of [QuestionRepository] that handles actual quiz questions.
 * Manages loading and storing questions from CSV files for the quiz application.
 */
class RealQuestionRepository : QuestionRepository {
    /** Stores the currently loaded questions */
    private var questions: List<QuizQuestion> = emptyList()

    /**
     * Gets the current list of loaded questions.
     * @return List of quiz questions, or empty list if no questions are loaded
     */
    override fun getQuestions(): List<QuizQuestion> = questions

    /**
     * Loads questions from a CSV file and stores them internally.
     * @param contentResolver Android content resolver to access the file
     * @param uri URI of the CSV file to load
     * @return List of loaded quiz questions, or empty list if loading fails
     */
    override fun loadQuestionsFromCsv(contentResolver: ContentResolver, uri: Uri): List<QuizQuestion> {
        questions = parseCsvFile(contentResolver, uri)
        return questions
    }
}
