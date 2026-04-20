package com.example.smartnotesai.ui.screens.notes

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.smartnotesai.data.model.Task
import com.example.smartnotesai.data.model.TaskPriority
import com.example.smartnotesai.ui.components.InsightCard
import com.example.smartnotesai.ui.components.TaskCard
import com.example.smartnotesai.ui.theme.SmartNotesAITheme
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun AddNotesScreen(
    onOpenHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val recognizedText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()

                if (recognizedText.isBlank()) {
                    viewModel.showVoiceInputError("No speech detected. Try again.")
                } else {
                    viewModel.appendRecognizedSpeech(recognizedText)
                }
            }

            Activity.RESULT_CANCELED -> {
                viewModel.showVoiceInputError("Voice input was cancelled.")
            }

            else -> {
                viewModel.showVoiceInputError("Unable to capture speech right now.")
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSpeechRecognizer(
                launcher = speechRecognizerLauncher,
                onError = viewModel::showVoiceInputError
            )
        } else {
            viewModel.showVoiceInputError("Microphone permission is required for voice input.")
        }
    }

    val onStartVoiceInput = {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            launchSpeechRecognizer(
                launcher = speechRecognizerLauncher,
                onError = viewModel::showVoiceInputError
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    AddNotesScreenContent(
        uiState = uiState,
        modifier = modifier,
        onNotesChange = viewModel::updateNotes,
        onExtractActions = viewModel::extractTasks,
        onClearResult = viewModel::clearResult,
        onOpenHome = onOpenHome,
        onStartVoiceInput = onStartVoiceInput
    )
}

@Composable
private fun AddNotesScreenContent(
    uiState: NotesUiState,
    onNotesChange: (String) -> Unit,
    onExtractActions: () -> Unit,
    onClearResult: () -> Unit,
    onOpenHome: () -> Unit,
    onStartVoiceInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Add Notes",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Paste unstructured notes and let Gemini pull out actions, priority, and timing.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = onNotesChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            placeholder = {
                Text("Example: Call client tomorrow, finish UI by Friday, maybe go to gym")
            },
            trailingIcon = {
                IconButton(
                    onClick = onStartVoiceInput,
                    enabled = !uiState.isLoading
                ) {
                    Icon(
                        painter = painterResource(id = android.R.drawable.ic_btn_speak_now),
                        contentDescription = "Speak note"
                    )
                }
            }
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = onExtractActions,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            enabled = uiState.canExtract
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Analyzing with Gemini")
            } else {
                Text(text = "Extract Actions")
            }
        }

        if (uiState.isLoading) {
            AiAnalysisCard(
                notePreview = uiState.notes,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.createdTasks.isNotEmpty()) {
            InsightCard(
                headline = "Tasks added",
                supportingText = "${uiState.createdTasks.size} tasks saved to ${uiState.savedDate}"
            )

            uiState.createdTasks.forEach { task ->
                TaskCard(task = task)
            }

            OutlinedButton(
                onClick = onOpenHome,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View tasks on Home")
            }

            OutlinedButton(
                onClick = onClearResult,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Clear result")
            }
        }
    }
}

@Composable
private fun AiAnalysisCard(
    notePreview: String,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        "Reading your note",
        "Separating action items",
        "Prioritizing deadlines"
    )
    var activeStep by remember { mutableIntStateOf(0) }
    val infiniteTransition = rememberInfiniteTransition(label = "ai-analysis")
    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge-scale"
    )
    val chipGlow by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chip-glow"
    )

    LaunchedEffect(Unit) {
        while (true) {
            delay(1400)
            activeStep = (activeStep + 1) % steps.size
        }
    }

    val previewText = notePreview
        .trim()
        .replace(Regex("\\s+"), " ")
        .let { text ->
            if (text.length > 92) {
                text.take(92) + "..."
            } else {
                text
            }
        }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer(
                                scaleX = badgeScale,
                                scaleY = badgeScale
                            )
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .offset(x = 18.dp, y = (-18).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .offset(x = (-20).dp, y = 16.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.55f))
                    )
                    Text(
                        text = "AI",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Gemini is analyzing your note",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Crossfade(
                        targetState = steps[activeStep],
                        label = "analysis-step"
                    ) { step ->
                        Text(
                            text = step,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            Surface(
                color = Color.White.copy(alpha = 0.55f),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = "\"$previewText\"",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AnalysisChip(
                    label = "Intent",
                    isActive = activeStep == 0,
                    emphasis = chipGlow
                )
                AnalysisChip(
                    label = "Priority",
                    isActive = activeStep == 1,
                    emphasis = chipGlow
                )
                AnalysisChip(
                    label = "Deadline",
                    isActive = activeStep == 2,
                    emphasis = chipGlow
                )
            }

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(MaterialTheme.shapes.large),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
            )

            Text(
                text = "Turning rough notes into structured tasks for a better result.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun AnalysisChip(
    label: String,
    isActive: Boolean,
    emphasis: Float,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + (0.14f * emphasis))
    val inactiveColor = Color.White.copy(alpha = 0.4f)

    Surface(
        modifier = modifier,
        color = if (isActive) activeColor else inactiveColor,
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun launchSpeechRecognizer(
    launcher: ActivityResultLauncher<Intent>,
    onError: (String) -> Unit
) {
    val speechIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your note")
    }

    runCatching {
        launcher.launch(speechIntent)
    }.onFailure {
        onError("Speech input is not available on this device.")
    }
}

@Preview(showBackground = true)
@Composable
private fun AddNotesScreenPreview() {
    SmartNotesAITheme {
        AddNotesScreenContent(
            uiState = NotesUiState(
                notes = "Call client tomorrow, finish UI by Friday",
                createdTasks = listOf(
                    Task(
                        id = 0,
                        task = "Call client",
                        priority = TaskPriority.HIGH,
                        deadline = "Tomorrow",
                        date = "17 Apr 2026"
                    )
                ),
                savedDate = "17 Apr 2026"
            ),
            onNotesChange = {},
            onExtractActions = {},
            onClearResult = {},
            onOpenHome = {},
            onStartVoiceInput = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun AddNotesScreenLoadingPreview() {
    SmartNotesAITheme {
        AddNotesScreenContent(
            uiState = NotesUiState(
                notes = "Call client tomorrow, finish UI by Friday",
                isLoading = true
            ),
            onNotesChange = {},
            onExtractActions = {},
            onClearResult = {},
            onOpenHome = {},
            onStartVoiceInput = {}
        )
    }
}
