package com.example.studentuse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ExercisePickerScreen(
    onExerciseSelected: (Exercise) -> Unit,
    onHistoryClick: () -> Unit = {},
    onAppSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }

    var streak by remember { mutableStateOf(0) }
    var squatRecord by remember { mutableStateOf(0) }
    var pushupRecord by remember { mutableStateOf(0) }
    var plankRecord by remember { mutableStateOf(0) }
    var lungeRecord by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        streak = repository.getStreak()
        squatRecord = repository.getRecord(Exercise.SQUAT, false)
        pushupRecord = repository.getRecord(Exercise.PUSHUP, false)
        plankRecord = repository.getRecord(Exercise.PLANK, true)
        lungeRecord = repository.getRecord(Exercise.LUNGE, false)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (streak >= 2) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFB71C1C)
                        )
                    ) {
                        Text(
                            text = "🔥 $streak ${pluralDays(streak)} подряд",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Выбери упражнение",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Exercise.entries.forEach { exercise ->
                    Button(
                        onClick = { onExerciseSelected(exercise) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(64.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "${exercise.emoji}  ${exercise.title}",
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onHistoryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "📊  История тренировок",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                val hasAnyRecord = squatRecord > 0 || pushupRecord > 0 ||
                        plankRecord > 0 || lungeRecord > 0

                if (hasAnyRecord) {
                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "🏆 Рекорды",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            if (squatRecord > 0) {
                                Text(
                                    "🦵 Приседания: $squatRecord",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (pushupRecord > 0) {
                                Text(
                                    "💪 Отжимания: $pushupRecord",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (plankRecord > 0) {
                                Text(
                                    "🧘 Планка: ${formatSeconds(plankRecord)}",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (lungeRecord > 0) {
                                Text(
                                    "🚶 Выпады: $lungeRecord",
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // === КНОПКА ⚙️ — С УЧЁТОМ СТАТУС-БАРА ===
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onAppSettingsClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xCC000000),
                        contentColor = Color.White
                    )
                ) {
                    Text(text = "⚙️", fontSize = 20.sp)
                }
            }
        }
    }
}

private fun pluralDays(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod100 in 11..14 -> "дней"
        mod10 == 1 -> "день"
        mod10 in 2..4 -> "дня"
        else -> "дней"
    }
}

private fun formatSeconds(seconds: Int): String {
    return if (seconds < 60) {
        "$seconds сек"
    } else {
        val min = seconds / 60
        val sec = seconds % 60
        if (sec == 0) "$min мин" else "$min мин $sec сек"
    }
}