/**
 * Main quiz application that allows users to load questions from a CSV file and take quizzes.
 * The app supports question grouping, score tracking, and immediate feedback.
 *
 * @author igalvit
 * @version 1.0
 */
package es.igalvit.quizappfromcsv

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import es.igalvit.quizappfromcsv.ui.theme.QuizAppFromCSVTheme
import es.igalvit.quizappfromcsv.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.io.InputStreamReader
import com.opencsv.CSVReader
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVParserBuilder
import es.igalvit.quizappfromcsv.data.QuestionRepository
import es.igalvit.quizappfromcsv.data.RealQuestionRepository

/**
 * Data class representing a quiz question with all its associated information.
 * This class is designed to be Parcelable for safe state handling in the Android lifecycle.
 *
 * @property questionText The main text of the question to be displayed to the user
 * @property options A list of 4 possible answers for the question (labeled A through D)
 * @property correctAnswer A string indicating the correct answer (must be "A", "B", "C", or "D")
 * @property group A string identifier for grouping related questions together (e.g., "1-50", "Grammar", etc.)
 */
@Parcelize
data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val group: String
) : Parcelable

/**
 * Data class representing the outcome of a user's answer to a quiz question.
 * This class is designed to be Parcelable for safe state handling in the Android lifecycle.
 *
 * @property isCorrect Boolean indicating whether the user's answer was correct
 * @property selectedAnswer The option (A, B, C, or D) that the user selected
 * @property correctAnswer The text of the correct answer for displaying feedback
 */
@Parcelize
data class AnswerResult(
    val isCorrect: Boolean,
    val selectedAnswer: String,
    val correctAnswer: String
) : Parcelable

/**
 * Sealed class representing the possible outcomes of parsing a CSV file.
 * This helps in handling different parsing scenarios and providing appropriate feedback.
 */
sealed class CsvParseResult {
    /**
     * Represents a successful parsing of the CSV file.
     * @property questions The list of parsed quiz questions
     */
    data class Success(val questions: List<QuizQuestion>) : CsvParseResult()

    /**
     * Represents a failure in parsing the CSV file.
     * @property message A user-friendly error message explaining what went wrong
     */
    data class Error(val message: String) : CsvParseResult()
}

/**
 * Parses a CSV file containing quiz questions and converts it into a list of QuizQuestion objects.
 *
 * The CSV file must follow this specific format:
 * - Must use semicolons (;) as separators
 * - Must contain a header row with these exact column names:
 *   questionText;option1;option2;option3;option4;correctAnswer;group
 * - correctAnswer must be A, B, C, or D corresponding to the option number
 * - group can be any string to categorize questions
 *
 * Example CSV content:
 * questionText;option1;option2;option3;option4;correctAnswer;group
 * "What is 2+2?";"3";"4";"5";"6";"B";"Math"
 *
 * @param contentResolver Android ContentResolver to access the file
 * @param uri URI pointing to the CSV file to parse
 * @return List<QuizQuestion> containing the parsed questions, or empty list if parsing fails
 * @throws IllegalArgumentException if the file is empty or missing required columns
 */
fun parseCsvFile(contentResolver: ContentResolver, uri: Uri): List<QuizQuestion> {
    try {
        val inputStream = contentResolver.openInputStream(uri) ?: return emptyList()
        val csvReaderBuilder = CSVReaderBuilder(InputStreamReader(inputStream))
            .withCSVParser(CSVParserBuilder()
                .withSeparator(';')
                .build())
        val reader = csvReaderBuilder.build()

        try {
            val rows: List<Array<String>> = reader.readAll()
            if (rows.isEmpty()) {
                throw IllegalArgumentException("CSV file is empty")
            }

            val header: Array<String> = rows[0]
            val requiredColumns = listOf("questionText", "option1", "option2", "option3", "option4", "correctAnswer", "group")

            // Validate that all required columns are present
            val missingColumns = requiredColumns.filter { !header.contains(it) }
            if (missingColumns.isNotEmpty()) {
                throw IllegalArgumentException("CSV file is missing required columns: ${missingColumns.joinToString()}")
            }

            val idxQuestion = header.indexOf("questionText")
            val idxOption1 = header.indexOf("option1")
            val idxOption2 = header.indexOf("option2")
            val idxOption3 = header.indexOf("option3")
            val idxOption4 = header.indexOf("option4")
            val idxCorrect = header.indexOf("correctAnswer")
            val idxGroup = header.indexOf("group")

            val questions = mutableListOf<QuizQuestion>()
            for (row in rows.drop(1)) {
                if (row.size < header.size) continue
                try {
                    questions.add(
                        QuizQuestion(
                            questionText = row[idxQuestion],
                            options = listOf(row[idxOption1], row[idxOption2], row[idxOption3], row[idxOption4]),
                            correctAnswer = row[idxCorrect],
                            group = row[idxGroup]
                        )
                    )
                } catch (e: Exception) {
                    // Skip malformed rows instead of failing the entire import
                    continue
                }
            }
            return questions
        } catch (e: Exception) {
            android.util.Log.e("CSV_PARSE", "Error parsing CSV file", e)
            return emptyList()
        } finally {
            reader.close()
        }
    } catch (e: Exception) {
        android.util.Log.e("CSV_PARSE", "Error reading CSV file", e)
        return emptyList()
    }
}

/**
 * Sorts a list of question groups in a natural order, making the groups appear in a logical sequence.
 * This is particularly useful for groups that contain numeric ranges in their names.
 *
 * Examples of sorting:
 * - Input: ["51-100", "1-50", "101-150"]
 * - Output: ["1-50", "51-100", "101-150"]
 *
 * Non-numeric groups will be sorted after numeric ones.
 *
 * @param groups List of group identifiers to sort
 * @return Sorted list of group names, with numeric ranges in natural order
 */
internal fun sortGroups(groups: List<String>): List<String> {
    return groups.sortedBy { group ->
        // Extract the first number from strings like "1-50", "51-100", etc.
        group.split("-").firstOrNull()?.trim()?.toIntOrNull() ?: Int.MAX_VALUE
    }
}

/**
 * Main Activity for the Quiz Application. This activity handles:
 * - Loading and parsing CSV files containing quiz questions
 * - Displaying questions and managing user responses
 * - Tracking scores and providing immediate feedback
 * - Filtering questions by groups
 * - State preservation across configuration changes
 *
 * The activity uses Jetpack Compose for the UI and follows MVVM architecture patterns.
 * It supports dynamic question loading and maintains quiz progress state.
 */
@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    /** Activity result launcher for picking CSV files */
    private lateinit var pickCsvFileLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>

    /** URI of the currently selected CSV file */
    private var selectedFileUri: Uri? = null

    // Add repository as a property that can be injected
    private var repository: QuestionRepository = RealQuestionRepository()
    private var questionState: (List<QuizQuestion>) -> Unit = {}

    // Add function to set repository for testing
    fun setRepository(testRepository: QuestionRepository) {
        repository = testRepository
    }

    // Add function to set questions directly for testing
    fun setQuestions(questions: List<QuizQuestion>) {
        questionState(questions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Activity Result Launcher first, before setting content
        pickCsvFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            selectedFileUri = uri
            if (uri != null) {
                try {
                    val parsedQuestions = repository.loadQuestionsFromCsv(contentResolver, uri)
                    if (parsedQuestions.isEmpty()) {
                        android.widget.Toast.makeText(
                            this,
                            "The CSV file is not properly formatted. Please ensure it has all required columns (questionText, option1-4, correctAnswer, group) and uses semicolons as separators.",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                    questionState(parsedQuestions)
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        this,
                        "Error reading CSV file: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                    questionState(emptyList())
                }
            }
        }

        setContent {
            QuizAppFromCSVTheme {
                // State management
                val questions = rememberSaveable { mutableStateOf<List<QuizQuestion>>(emptyList()) }
                val allQuestions = rememberSaveable { mutableStateOf<List<QuizQuestion>>(emptyList()) }
                val currentQuestionIndex = rememberSaveable { mutableStateOf(0) }
                val score = rememberSaveable { mutableStateOf(0) }
                val incorrectCount = rememberSaveable { mutableStateOf(0) }
                val showMessage = rememberSaveable { mutableStateOf(false) }
                val selectedGroup = rememberSaveable { mutableStateOf("All") }
                val hasAnswered = rememberSaveable { mutableStateOf(false) }

                // Answer states
                val isCorrect = rememberSaveable { mutableStateOf<Boolean?>(null) }
                val selectedAnswer = rememberSaveable { mutableStateOf<String?>(null) }
                val correctAnswer = rememberSaveable { mutableStateOf<String?>(null) }

                // Timer state
                val elapsedSeconds = rememberSaveable { mutableStateOf(0L) }
                val isTimerRunning = rememberSaveable { mutableStateOf(false) }

                // Timer effect
                LaunchedEffect(isTimerRunning.value) {
                    while (isTimerRunning.value) {
                        delay(1000)
                        elapsedSeconds.value++
                    }
                }

                // Format time as MM:SS
                val formattedTime = remember(elapsedSeconds.value) {
                    String.format("%02d:%02d",
                        elapsedSeconds.value / 60,
                        elapsedSeconds.value % 60
                    )
                }

                // Compute the answer result from individual states
                val lastAnswerResult = remember {
                    mutableStateOf<AnswerResult?>(null)
                }.apply {
                    if (isCorrect.value != null && selectedAnswer.value != null && correctAnswer.value != null) {
                        value = AnswerResult(
                            isCorrect = isCorrect.value!!,
                            selectedAnswer = selectedAnswer.value!!,
                            correctAnswer = correctAnswer.value!!
                        )
                    }
                }

                val scope = rememberCoroutineScope()

                // Update question state callback
                questionState = { newQuestions ->
                    allQuestions.value = newQuestions
                    questions.value = newQuestions
                    currentQuestionIndex.value = 0
                    score.value = 0
                    incorrectCount.value = 0
                    hasAnswered.value = false
                    // Reset timer
                    elapsedSeconds.value = 0
                    isTimerRunning.value = !newQuestions.isEmpty()
                    // Reset answer states
                    isCorrect.value = null
                    selectedAnswer.value = null
                    correctAnswer.value = null
                }

                // Update the onClick handler for answer buttons
                fun handleAnswer(letterAnswer: String, isAnswerCorrect: Boolean, correctAnswerText: String) {
                    if (!hasAnswered.value) {
                        hasAnswered.value = true
                        showMessage.value = true

                        if (isAnswerCorrect) {
                            score.value++
                        } else {
                            incorrectCount.value++
                        }

                        // Update individual states
                        isCorrect.value = isAnswerCorrect
                        selectedAnswer.value = letterAnswer
                        correctAnswer.value = correctAnswerText

                        scope.launch {
                            delay(2000)
                            showMessage.value = false
                            if (currentQuestionIndex.value < questions.value.size - 1) {
                                currentQuestionIndex.value++
                                hasAnswered.value = false
                            }

                            // Check if all questions in the group have been answered
                            val answeredAll = (score.value + incorrectCount.value) >= questions.value.size
                            if (answeredAll) {
                                isTimerRunning.value = false
                            }
                        }
                    }
                }

                // UI setup
                Box(modifier = Modifier.fillMaxSize()) {
                    /**
                     * Main scaffold containing the quiz content
                     */
                    Scaffold(
                        modifier = Modifier.fillMaxSize()
                    ) { innerPadding ->
                        /**
                         * Main content column containing either quiz interface or welcome screen
                         */
                        Column(modifier = Modifier.padding(innerPadding)) {
                            if (questions.value.isNotEmpty()) {
                                // Progress and score display
                                val totalQuestions = questions.value.size
                                val questionNumber = currentQuestionIndex.value + 1
                                val progress = questionNumber.toFloat() / totalQuestions.toFloat()
                                val animatedProgress by animateFloatAsState(
                                    targetValue = progress,
                                    label = "Progress"
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    // Timer display
                                    Text(
                                        text = "Time: $formattedTime",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        textAlign = TextAlign.Center
                                    )

                                    // Progress bar
                                    LinearProgressIndicator(
                                        progress = animatedProgress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Score statistics in a nice layout
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0xFFE3F2FD),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Question Progress
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "$questionNumber/$totalQuestions",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Question",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        // Correct Answers
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${score.value}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF4CAF50)
                                            )
                                            Text(
                                                text = "Correct",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        // Incorrect Answers
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${incorrectCount.value}",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF44336)
                                            )
                                            Text(
                                                text = "Incorrect",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }

                                // Question text display
                                val currentQuestion = questions.value[currentQuestionIndex.value]
                                Text(
                                    text = currentQuestion.questionText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    modifier = Modifier
                                        .padding(vertical = 16.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )

                                // Answer options
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    currentQuestion.options.forEachIndexed { index, option ->
                                        val letterAnswer = ('A' + index).toString()
                                        val isAnswerCorrect = index == (currentQuestion.correctAnswer[0] - 'A')
                                        val containerColor = when {
                                            !hasAnswered.value -> ButtonDefaults.buttonColors().containerColor
                                            isAnswerCorrect -> Color(0xFF4CAF50) // Green for correct
                                            letterAnswer == selectedAnswer.value -> Color(0xFFF44336) // Red for wrong
                                            else -> ButtonDefaults.buttonColors().containerColor
                                        }

                                        Button(
                                            onClick = {
                                                handleAnswer(
                                                    letterAnswer,
                                                    isAnswerCorrect,
                                                    currentQuestion.options[currentQuestion.correctAnswer[0] - 'A']
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = containerColor
                                            ),
                                            enabled = !hasAnswered.value || hasAnswered.value,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            Text(
                                                "${letterAnswer}: $option",
                                                color = Color.White
                                            )
                                        }
                                    }
                                }

                                // Navigation and control buttons
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .background(
                                            Color(0xFFF5F5F5),
                                            RoundedCornerShape(28.dp)
                                        )
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous button
                                    FilledTonalButton(
                                        onClick = {
                                            if (currentQuestionIndex.value > 0) {
                                                currentQuestionIndex.value--
                                                hasAnswered.value = false
                                            }
                                        },
                                        enabled = currentQuestionIndex.value > 0,
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFFE57373),
                                            disabledContainerColor = Color(0xFFFFCDD2)
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowBack,
                                            contentDescription = "Previous Question",
                                            tint = Color.White
                                        )
                                    }

                                    // Group filter button
                                    var expanded by remember { mutableStateOf(false) }
                                    val groups = remember(questions.value) {
                                        val allGroups = questions.value.map { it.group }.distinct()
                                        listOf("All") + sortGroups(allGroups)
                                    }

                                    Box {
                                        FilledTonalButton(
                                            onClick = { expanded = true },
                                            modifier = Modifier.size(56.dp),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = Color(0xFF9575CD) // Purple
                                            ),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Menu,
                                                contentDescription = "Filter Groups",
                                                tint = Color.White
                                            )
                                        }
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false }
                                        ) {
                                            groups.forEach { group ->
                                                DropdownMenuItem(
                                                    text = { Text(group) },
                                                    onClick = {
                                                        selectedGroup.value = group
                                                        expanded = false
                                                        // Reset timer when changing groups
                                                        elapsedSeconds.value = 0
                                                        isTimerRunning.value = true
                                                        if (group == "All") {
                                                            questions.value = allQuestions.value
                                                        } else {
                                                            questions.value = allQuestions.value.filter { it.group == group }
                                                        }
                                                        currentQuestionIndex.value = 0
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Pick new file button
                                    FloatingActionButton(
                                        onClick = {
                                            pickCsvFileLauncher.launch(arrayOf(
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "application/csv"
                                            ))
                                        },
                                        containerColor = Color(0xFF64B5F6), // Light Blue
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Pick CSV File",
                                            tint = Color.White
                                        )
                                    }

                                    // Next button
                                    FilledTonalButton(
                                        onClick = {
                                            if (currentQuestionIndex.value < questions.value.size - 1) {
                                                currentQuestionIndex.value++
                                                hasAnswered.value = false
                                            }
                                        },
                                        enabled = currentQuestionIndex.value < questions.value.size - 1,
                                        modifier = Modifier.size(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF81C784), // Light Green
                                            disabledContainerColor = Color(0xFFC8E6C9) // Very Light Green
                                        ),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ArrowForward,
                                            contentDescription = "Next Question",
                                            tint = Color.White
                                        )
                                    }
                                }
                            } else {
                                /**
                                 * Welcome screen with file picker
                                 * Displayed when no questions are loaded
                                 */
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.app_logo),
                                            contentDescription = "Quiz App Logo",
                                            modifier = Modifier
                                                .size(120.dp)
                                                .padding(bottom = 24.dp),
                                            tint = Color.Unspecified
                                        )
                                        Text(
                                            text = "Quiz App",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 24.sp,
                                            modifier = Modifier.padding(bottom = 16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        Button(
                                            onClick = {
                                                pickCsvFileLauncher.launch(arrayOf(
                                                    "text/csv",
                                                    "text/comma-separated-values",
                                                    "application/csv"
                                                ))
                                            },
                                            modifier = Modifier.padding(top = 16.dp)
                                        ) {
                                            Text(text = "Pick CSV File")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    /**
                     * Animated feedback message
                     */
                    AnimatedVisibility(
                        visible = showMessage.value,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        lastAnswerResult.value?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (result.isCorrect) Color(0xFF4CAF50)
                                        else Color(0xFFF44336),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = if (result.isCorrect) {
                                        "Correct!"
                                    } else {
                                        "Incorrect. The correct answer was: ${result.correctAnswer}"
                                    },
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    /**
                     * Copyright notice
                     * Displayed at the bottom of all screens
                     */
                    Text(
                        text = "© 2025 This work is dedicated to the public domain under CC0 1.0 Universal",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

