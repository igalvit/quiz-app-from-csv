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
import es.igalvit.quizappfromcsv.data.QuestionRepository
import es.igalvit.quizappfromcsv.data.RealQuestionRepository

/**
 * Data class representing a single quiz question with its options and metadata.
 *
 * @property questionText The text of the question to be displayed
 * @property options List of possible answers (4 options: A, B, C, D)
 * @property correctAnswer The correct answer identifier (A, B, C, or D)
 * @property group The question group identifier for filtering
 */
@Parcelize
data class QuizQuestion(
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val group: String
) : Parcelable

/**
 * Data class representing the result of the last answer.
 *
 * @property isCorrect Whether the answer was correct
 * @property selectedAnswer The answer selected by the user
 * @property correctAnswer The correct answer
 */
@Parcelize
data class AnswerResult(
    val isCorrect: Boolean,
    val selectedAnswer: String,
    val correctAnswer: String
) : Parcelable

/**
 * Parses a CSV file to create a list of quiz questions.
 * Expected CSV format: questionText,option1,option2,option3,option4,correctAnswer,group
 *
 * @param contentResolver ContentResolver to access the file
 * @param uri URI of the CSV file to parse
 * @return List of QuizQuestion objects, or empty list if parsing fails
 */
fun parseCsvFile(contentResolver: ContentResolver, uri: Uri): List<QuizQuestion> {
    val questions = mutableListOf<QuizQuestion>()
    val inputStream = contentResolver.openInputStream(uri) ?: return emptyList()
    val reader = CSVReader(InputStreamReader(inputStream))
    val rows: List<Array<String>> = reader.readAll()
    if (rows.isEmpty()) return emptyList()
    val header: Array<String> = rows[0]
    val idxQuestion = header.indexOf("questionText")
    val idxOption1 = header.indexOf("option1")
    val idxOption2 = header.indexOf("option2")
    val idxOption3 = header.indexOf("option3")
    val idxOption4 = header.indexOf("option4")
    val idxCorrect = header.indexOf("correctAnswer")
    val idxGroup = header.indexOf("group")
    for (row in rows.drop(1)) {
        if (row.size < header.size) continue
        questions.add(
            QuizQuestion(
                questionText = row[idxQuestion],
                options = listOf(row[idxOption1], row[idxOption2], row[idxOption3], row[idxOption4]),
                correctAnswer = row[idxCorrect],
                group = row[idxGroup]
            )
        )
    }
    return questions
}

/**
 * Sorts question groups in natural order, handling numeric ranges in group names.
 * For example: "1-50" comes before "51-100"
 *
 * @param groups List of group names to sort
 * @return Sorted list of group names
 */
internal fun sortGroups(groups: List<String>): List<String> {
    return groups.sortedBy { group ->
        // Extract the first number from strings like "1-50", "51-100", etc.
        group.split("-").firstOrNull()?.trim()?.toIntOrNull() ?: Int.MAX_VALUE
    }
}

/**
 * Main activity of the Quiz application.
 * Handles file picking, question display, answer tracking, and score management.
 * Uses Jetpack Compose for the UI.
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
                val parsedQuestions = repository.loadQuestionsFromCsv(contentResolver, uri)
                questionState(parsedQuestions)
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

                // Split answer result into individual saveable states
                val isCorrect = rememberSaveable { mutableStateOf<Boolean?>(null) }
                val selectedAnswer = rememberSaveable { mutableStateOf<String?>(null) }
                val correctAnswer = rememberSaveable { mutableStateOf<String?>(null) }

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

                val hasAnswered = rememberSaveable { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                // Update question state callback
                questionState = { newQuestions ->
                    allQuestions.value = newQuestions
                    questions.value = newQuestions
                    currentQuestionIndex.value = 0
                    score.value = 0
                    incorrectCount.value = 0
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
