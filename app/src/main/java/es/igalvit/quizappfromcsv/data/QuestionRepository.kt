package es.igalvit.quizappfromcsv.data

import android.content.ContentResolver
import android.net.Uri
import es.igalvit.quizappfromcsv.QuizQuestion

/**
 * Repository interface for managing quiz questions.
 * Provides methods to retrieve questions and load them from CSV files.
 */
interface QuestionRepository {
    /**
     * Gets the current list of loaded questions.
     * @return List of quiz questions, or empty list if no questions are loaded
     */
    fun getQuestions(): List<QuizQuestion>

    /**
     * Loads questions from a CSV file.
     * @param contentResolver Android content resolver to access the file
     * @param uri URI of the CSV file to load
     * @return List of loaded quiz questions, or empty list if loading fails
     */
    fun loadQuestionsFromCsv(contentResolver: ContentResolver, uri: Uri): List<QuizQuestion>
}
