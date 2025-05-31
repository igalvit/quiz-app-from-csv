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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
data class AnswerResult(
    val isCorrect: Boolean,
    val selectedAnswer: String,
    val correctAnswer: String
)

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var questionState: (List<QuizQuestion>) -> Unit = {}

        // Initialize Activity Result Launcher first
        pickCsvFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            selectedFileUri = uri
            if (uri != null) {
                val parsedQuestions = parseCsvFile(contentResolver, uri)
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
                val lastAnswerResult = rememberSaveable { mutableStateOf<AnswerResult?>(null) }
                val hasAnswered = rememberSaveable { mutableStateOf(false) }
                val scope = rememberCoroutineScope()

                // Update question state callback
                questionState = { newQuestions ->
                    allQuestions.value = newQuestions
                    questions.value = newQuestions
                    currentQuestionIndex.value = 0
                    score.value = 0
                    incorrectCount.value = 0
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

                                // Progress display
                                val totalQuestions = questions.value.size
                                val questionNumber = currentQuestionIndex.value + 1
                                Text(
                                    text = "Current: $questionNumber | Total: $totalQuestions -- Correct: ${score.value} | Incorrect: ${incorrectCount.value}",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 16.dp),
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
                                        val isCorrect = index == (currentQuestion.correctAnswer[0] - 'A')
                                        val containerColor = when {
                                            !hasAnswered.value -> ButtonDefaults.buttonColors().containerColor
                                            isCorrect -> Color(0xFF4CAF50) // Green for correct
                                            letterAnswer == lastAnswerResult.value?.selectedAnswer -> Color(0xFFF44336) // Red for wrong
                                            else -> ButtonDefaults.buttonColors().containerColor
                                        }

                                        Button(
                                            onClick = {
                                                if (!hasAnswered.value) {
                                                    hasAnswered.value = true
                                                    showMessage.value = true

                                                    if (isCorrect) {
                                                        score.value++
                                                    } else {
                                                        incorrectCount.value++
                                                    }

                                                    lastAnswerResult.value = AnswerResult(
                                                        isCorrect = isCorrect,
                                                        selectedAnswer = letterAnswer,
                                                        correctAnswer = currentQuestion.options[currentQuestion.correctAnswer[0] - 'A']
                                                    )

                                                    scope.launch {
                                                        delay(2000)
                                                        showMessage.value = false
                                                        if (currentQuestionIndex.value < questions.value.size - 1) {
                                                            currentQuestionIndex.value++
                                                            hasAnswered.value = false
                                                        }
                                                    }
                                                }
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
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Previous button
                                    Button(
                                        onClick = {
                                            if (currentQuestionIndex.value > 0) {
                                                currentQuestionIndex.value--
                                                hasAnswered.value = false
                                            }
                                        },
                                        enabled = currentQuestionIndex.value > 0,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.DarkGray
                                        )
                                    ) {
                                        Text("Previous")
                                    }

                                    // Group filter dropdown
                                    var expanded by remember { mutableStateOf(false) }
                                    val groups = remember(questions.value) {
                                        val allGroups = questions.value.map { it.group }.distinct()
                                        listOf("All") + sortGroups(allGroups)
                                    }

                                    Box {
                                        Button(
                                            onClick = { expanded = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color.LightGray
                                            )
                                        ) {
                                            Text(selectedGroup.value)
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
                                                        // Filter questions by group
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
                                    Button(
                                        onClick = {
                                            pickCsvFileLauncher.launch(arrayOf(
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "application/csv"
                                            ))
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF90CAF9) // Light Blue
                                        )
                                    ) {
                                        Text("Pick CSV File")
                                    }

                                    // Next button
                                    Button(
                                        onClick = {
                                            if (currentQuestionIndex.value < questions.value.size - 1) {
                                                currentQuestionIndex.value++
                                                hasAnswered.value = false
                                            }
                                        },
                                        enabled = currentQuestionIndex.value < questions.value.size - 1,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF4CAF50) // Green
                                        )
                                    ) {
                                        Text("Next")
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
