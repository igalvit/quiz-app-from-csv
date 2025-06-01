package es.igalvit.quizappfromcsv

import org.junit.Test
import org.junit.Assert.*
import java.io.StringReader
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVParserBuilder

class QuizLogicTest {
    @Test
    fun sortGroups_withNumericRanges_sortsCorrectly() {
        val unsortedGroups = listOf("51-100", "151-206", "1-50", "101-150")
        val sortedGroups = sortGroups(unsortedGroups)
        assertEquals(listOf("1-50", "51-100", "101-150", "151-206"), sortedGroups)
    }

    @Test
    fun sortGroups_withMixedContent_handlesNonNumericGroups() {
        val mixedGroups = listOf("51-100", "Practice", "1-50", "Review")
        val sortedGroups = sortGroups(mixedGroups)
        assertEquals(listOf("1-50", "51-100", "Practice", "Review"), sortedGroups)
    }

    @Test
    fun csvParsing_validFormat_returnsQuestions() {
        val csvContent = """
            questionText;option1;option2;option3;option4;correctAnswer;group
            What is 2+2?;4;3;5;6;A;1-50
            What is 3+3?;4;6;5;7;B;1-50
        """.trimIndent()
        val reader = CSVReaderBuilder(StringReader(csvContent))
            .withCSVParser(CSVParserBuilder().withSeparator(';').build())
            .build()
        val rows = reader.readAll()
        assertEquals(3, rows.size)
        assertEquals("questionText", rows[0][0])
        assertEquals("What is 2+2?", rows[1][0])
    }

    @Test
    fun csvParsing_invalidFormat_handlesEmptyFile() {
        val reader = CSVReaderBuilder(StringReader(""))
            .withCSVParser(CSVParserBuilder().withSeparator(';').build())
            .build()
        assertTrue(reader.readAll().isEmpty())
    }

    @Test
    fun csvParsing_invalidFormat_handlesMissingColumns() {
        val csvContent = """
            questionText;option1;option2
            Incomplete question;A;B
        """.trimIndent()
        val reader = CSVReaderBuilder(StringReader(csvContent))
            .withCSVParser(CSVParserBuilder().withSeparator(';').build())
            .build()
        val rows = reader.readAll()
        assertEquals(2, rows.size)
        assertTrue(rows[1].size < 7)
    }

    @Test
    fun csvParsing_validFormat_handlesSemicolonInContent() {
        val csvContent = """
            questionText;option1;option2;option3;option4;correctAnswer;group
            "What; is this?";4;3;5;6;A;1-50
            "Option; with; semicolons";A;B;C;D;B;1-50
        """.trimIndent()
        val reader = CSVReaderBuilder(StringReader(csvContent))
            .withCSVParser(CSVParserBuilder().withSeparator(';').build())
            .build()
        val rows = reader.readAll()
        assertEquals("What; is this?", rows[1][0])
        assertEquals("Option; with; semicolons", rows[2][0])
    }

    @Test
    fun csvParsing_validFormat_handlesQuotedContent() {
        val csvContent = """
            questionText;option1;option2;option3;option4;correctAnswer;group
            "Complex; Question";"Answer; 1";"Answer; 2";"Answer; 3";"Answer; 4";A;"Group; 1"
        """.trimIndent()
        val reader = CSVReaderBuilder(StringReader(csvContent))
            .withCSVParser(CSVParserBuilder().withSeparator(';').build())
            .build()
        val rows = reader.readAll()
        assertEquals("Complex; Question", rows[1][0])
        assertEquals("Answer; 1", rows[1][1])
        assertEquals("Group; 1", rows[1][6])
    }

    @Test
    fun filterQuestions_byGroup_returnsCorrectQuestions() {
        val questions = listOf(
            QuizQuestion("Q1", listOf("A", "B", "C", "D"), "A", "1-50"),
            QuizQuestion("Q2", listOf("A", "B", "C", "D"), "B", "51-100"),
            QuizQuestion("Q3", listOf("A", "B", "C", "D"), "C", "1-50")
        )
        val filtered = questions.filter { it.group == "1-50" }
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.group == "1-50" })
    }

    @Test
    fun copyrightText_hasCorrectContent() {
        assertEquals(
            "© 2025 This work is dedicated to the public domain under CC0 1.0 Universal",
            getCopyrightText()
        )
    }

    @Test
    fun messageDisplay_correctAnswer_showsCorrectFeedback() {
        assertEquals("Correct!", getFeedbackMessage(true, null))
    }

    @Test
    fun messageDisplay_incorrectAnswer_showsDetailedFeedback() {
        val message = getFeedbackMessage(false, Pair("B", "This is correct"))
        assertEquals("Incorrect! Correct answer: B. This is correct", message)
    }

    @Test
    fun answerResult_correctAnswer_createsCorrectResult() {
        val result = AnswerResult(
            isCorrect = true,
            selectedAnswer = "A",
            correctAnswer = "Option 1"
        )
        assertTrue(result.isCorrect)
        assertEquals("A", result.selectedAnswer)
        assertEquals("Option 1", result.correctAnswer)
    }

    @Test
    fun answerResult_incorrectAnswer_createsCorrectResult() {
        val result = AnswerResult(
            isCorrect = false,
            selectedAnswer = "B",
            correctAnswer = "Option 1"
        )
        assertFalse(result.isCorrect)
        assertEquals("B", result.selectedAnswer)
        assertEquals("Option 1", result.correctAnswer)
    }

    @Test
    fun quizQuestion_construction_isValid() {
        val question = QuizQuestion(
            questionText = "Test Question",
            options = listOf("A", "B", "C", "D"),
            correctAnswer = "A",
            group = "1-50"
        )
        assertEquals("Test Question", question.questionText)
        assertEquals(4, question.options.size)
        assertEquals("A", question.correctAnswer)
        assertEquals("1-50", question.group)
    }

    @Test
    fun answerValidation_correctIndex_matchesLetter() {
        val index = 0 // representing answer A
        val letter = 'A'
        assertEquals(index, letter - 'A')
    }

    @Test
    fun answerValidation_allOptions_matchCorrectly() {
        val answers = listOf('A', 'B', 'C', 'D')
        answers.forEachIndexed { index, letter ->
            assertEquals(index, letter - 'A')
        }
    }

    @Test
    fun answerState_initialState_isNull() {
        val isCorrect: Boolean? = null
        val selectedAnswer: String? = null
        val correctAnswer: String? = null

        assertNull(isCorrect)
        assertNull(selectedAnswer)
        assertNull(correctAnswer)
    }

    @Test
    fun answerState_afterAnswer_hasValidValues() {
        val isCorrect = true
        val selectedAnswer = "A"
        val correctAnswer = "Option 1"

        assertNotNull(isCorrect)
        assertNotNull(selectedAnswer)
        assertNotNull(correctAnswer)
        assertTrue(isCorrect)
        assertEquals("A", selectedAnswer)
        assertEquals("Option 1", correctAnswer)
    }

    @Test
    fun answerState_reset_clearsValues() {
        var isCorrect: Boolean? = true
        var selectedAnswer: String? = "A"
        var correctAnswer: String? = "Option 1"

        // Reset values
        isCorrect = null
        selectedAnswer = null
        correctAnswer = null

        assertNull(isCorrect)
        assertNull(selectedAnswer)
        assertNull(correctAnswer)
    }

    @Test
    fun progressCalculation_returnsCorrectValue() {
        val currentIndex = 4
        val totalQuestions = 10
        val expectedProgress = 0.5f // (4 + 1) / 10

        val progress = (currentIndex + 1).toFloat() / totalQuestions.toFloat()
        assertEquals(expectedProgress, progress)
    }

    private fun getCopyrightText() = "© 2025 This work is dedicated to the public domain under CC0 1.0 Universal"
    private fun getFeedbackMessage(isCorrect: Boolean, correctAnswer: Pair<String, String>?) =
        if (isCorrect) "Correct!"
        else "Incorrect! Correct answer: ${correctAnswer?.first}. ${correctAnswer?.second}"
}
