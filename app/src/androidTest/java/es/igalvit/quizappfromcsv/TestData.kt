package es.igalvit.quizappfromcsv

object TestData {
    val mockQuestions = listOf(
        QuizQuestion(
            questionText = "What is 2+2?",
            options = listOf("4", "3", "5", "6"),
            correctAnswer = "A",
            group = "1-50"
        ),
        QuizQuestion(
            questionText = "What is the capital of France?",
            options = listOf("London", "Paris", "Berlin", "Madrid"),
            correctAnswer = "B",
            group = "51-100"
        ),
        QuizQuestion(
            questionText = "What is H2O?",
            options = listOf("Salt", "Water", "Sugar", "Oil"),
            correctAnswer = "B",
            group = "1-50"
        )
    )

    fun getMockCsvContent(): String = """
        questionText,option1,option2,option3,option4,correctAnswer,group
        What is 2+2?,4,3,5,6,A,1-50
        What is the capital of France?,London,Paris,Berlin,Madrid,B,51-100
        What is H2O?,Salt,Water,Sugar,Oil,B,1-50
    """.trimIndent()
}
