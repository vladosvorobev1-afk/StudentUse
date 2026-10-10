package com.example.studentuse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { WorkoutRepository(context) }
    val scope = rememberCoroutineScope()

    var records by remember { mutableStateOf<List<WorkoutRecord>>(emptyList()) }
    var last7Days by remember { mutableStateOf<List<WorkoutRecord>>(emptyList()) }
    var streak by remember { mutableStateOf(0) }
    var squatRecord by remember { mutableStateOf(0) }
    var pushupRecord by remember { mutableStateOf(0) }
    var plankRecord by remember { mutableStateOf(0) }
    var lungeRecord by remember { mutableStateOf(0) }

    var loading by remember { mutableStateOf(true) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showDeleteLastDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        records = repository.getAll()
        last7Days = repository.getLast7Days()
        streak = repository.getStreak()
        squatRecord = repository.getRecord(Exercise.SQUAT, false)
        pushupRecord = repository.getRecord(Exercise.PUSHUP, false)
        plankRecord = repository.getRecord(Exercise.PLANK, true)
        lungeRecord = repository.getRecord(Exercise.LUNGE, false)
        loading = false
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = "История тренировок",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // === СТАТИСТИКА ЗА 7 ДНЕЙ ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "За последние 7 дней",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    val totalReps = last7Days.filter { it.reps > 0 }.sumOf { it.reps }
                    val totalSeconds = last7Days.filter { it.seconds > 0 }.sumOf { it.seconds }
                    val totalWorkouts = last7Days.size

                    Text(
                        text = "Тренировок: $totalWorkouts",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Всего повторов: $totalReps",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Планка: ${formatSeconds(totalSeconds)}",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (streak >= 2) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "🔥 Серия: $streak ${pluralDays(streak)} подряд",
                            fontSize = 15.sp,
                            color = Color(0xFFFF6F00),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // === РЕКОРДЫ ===
            val hasAnyRecord = squatRecord > 0 || pushupRecord > 0 ||
                    plankRecord > 0 || lungeRecord > 0

            if (hasAnyRecord) {
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🏆 Личные рекорды",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))

                        if (squatRecord > 0) {
                            Text(
                                text = "🦵 Приседания: $squatRecord",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (pushupRecord > 0) {
                            Text(
                                text = "💪 Отжимания: $pushupRecord",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (plankRecord > 0) {
                            Text(
                                text = "🧘 Планка: ${formatSeconds(plankRecord)}",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        if (lungeRecord > 0) {
                            Text(
                                text = "🚶 Выпады: $lungeRecord",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === СПИСОК ===
            if (loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (records.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Пока нет записей.\nСделай первую тренировку!",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records, key = { it.id }) { record ->
                        RecordRow(record)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // === КНОПКИ ===
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showDeleteLastDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    enabled = records.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF6C00),
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Отменить\nпоследнюю", fontSize = 14.sp)
                }

                Button(
                    onClick = { showClearDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF8B0000),
                        contentColor = Color.White
                    )
                ) {
                    Text("Очистить\nвсё", fontSize = 14.sp)
                }

                Button(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Назад", fontSize = 14.sp)
                }
            }
        }
    }

    // === ДИАЛОГ: УДАЛИТЬ ПОСЛЕДНЮЮ ===
    if (showDeleteLastDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteLastDialog = false },
            title = { Text("Отменить последнюю запись?") },
            text = { Text("Удалится самая свежая запись из истории. Рекорды и серия не изменятся.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            records.firstOrNull()?.let { last ->
                                repository.deleteById(last.id)
                            }
                            records = repository.getAll()
                            last7Days = repository.getLast7Days()
                            showDeleteLastDialog = false
                        }
                    }
                ) {
                    Text("Удалить", color = Color(0xFFEF6C00))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteLastDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // === ДИАЛОГ: ОЧИСТИТЬ ВСЁ ===
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Очистить всю историю?") },
            text = { Text("Все записи будут удалены. Рекорды и серия сохранятся. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            repository.clearAll()
                            records = emptyList()
                            last7Days = emptyList()
                            showClearDialog = false
                        }
                    }
                ) {
                    Text("Очистить", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun RecordRow(record: WorkoutRecord) {
    val exerciseName = when (record.exercise) {
        "SQUAT" -> "🦵 Приседания"
        "PUSHUP" -> "💪 Отжимания"
        "PLANK" -> "🧘 Планка"
        "LUNGE" -> "🚶 Выпады"
        else -> record.exercise
    }

    val resultText = if (record.reps > 0) {
        "${record.reps} повторов"
    } else {
        formatSeconds(record.seconds)
    }

    val dateText = formatDate(record.timestamp)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = exerciseName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = resultText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val formatter = SimpleDateFormat("dd.MM.yyyy, HH:mm", Locale("ru", "RU"))
    return formatter.format(Date(timestamp))
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