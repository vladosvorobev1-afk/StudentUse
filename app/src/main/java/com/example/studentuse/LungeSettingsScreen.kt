package com.example.studentuse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LungeSettingsScreen(
    targetReps: Int,
    onTargetRepsChange: (Int) -> Unit,
    saySetComplete: Boolean,
    onSaySetCompleteChange: (Boolean) -> Unit,
    vibrateOnSetComplete: Boolean,
    onVibrateOnSetCompleteChange: (Boolean) -> Unit,
    sayBack: Boolean,
    onSayBackChange: (Boolean) -> Unit,
    sayDeeper: Boolean,
    onSayDeeperChange: (Boolean) -> Unit,
    sayGood: Boolean,
    onSayGoodChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHowTo by remember { mutableStateOf(false) }

    val textColor = MaterialTheme.colorScheme.onBackground
    val subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Text(
                text = "Настройки выпадов",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Button(
                onClick = { showHowTo = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1565C0),
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "❓ Как правильно снимать",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Целевое количество повторов",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = {
                    val next = (targetReps - AppSettings.TARGET_REPS_STEP)
                        .coerceAtLeast(AppSettings.TARGET_REPS_MIN)
                    onTargetRepsChange(next)
                }) {
                    Text("−", fontSize = 20.sp)
                }

                Text(
                    text = if (targetReps == AppSettings.TARGET_REPS_NONE) "Без цели"
                    else targetReps.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.width(100.dp)
                )

                Button(onClick = {
                    val next = (targetReps + AppSettings.TARGET_REPS_STEP)
                        .coerceAtMost(AppSettings.TARGET_REPS_MAX)
                    onTargetRepsChange(next)
                }) {
                    Text("+", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onTargetRepsChange(AppSettings.TARGET_REPS_NONE)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (targetReps == AppSettings.TARGET_REPS_NONE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (targetReps == AppSettings.TARGET_REPS_NONE)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Без цели")
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            SettingToggle(
                title = "Говорить «Подход завершён»",
                checked = saySetComplete,
                enabled = targetReps > 0,
                onCheckedChange = onSaySetCompleteChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(Modifier.height(12.dp))

            SettingToggle(
                title = "Вибрация для тренера/партнёра",
                checked = vibrateOnSetComplete,
                onCheckedChange = onVibrateOnSetCompleteChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text(
                text = "Голосовые подсказки",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SettingToggle(
                title = "«Держи спину»",
                checked = sayBack,
                onCheckedChange = onSayBackChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(Modifier.height(12.dp))

            SettingToggle(
                title = "«Ниже»",
                checked = sayDeeper,
                onCheckedChange = onSayDeeperChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(Modifier.height(12.dp))

            SettingToggle(
                title = "«Хорошо»",
                checked = sayGood,
                onCheckedChange = onSayGoodChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Назад", fontSize = 18.sp)
            }
        }
    }

    if (showHowTo) {
        AlertDialog(
            onDismissRequest = { showHowTo = false },
            title = { Text("Как правильно снимать", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "1. Поставь телефон на подставку высотой 50–60 см от пола.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "2. Встань вполоборота — примерно 60° от камеры (не строго сбоку, не прямо).",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "3. Встань так, чтобы камера видела тебя в полный рост.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "4. Сделай выпад. Подними ногу при возврате — тогда повтор засчитается.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "📱 ВАЖНО: камера должна быть на высоте 50–60 см. Если поставить на пол — углы искажаются, счёт не работает.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "💡 Если счёт не идёт — проверь ракурс: телефон вполоборота, тело в профиль.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "🔄 Если счёт случайно сбился — нажми «Сброс» на экране и продолжай с чистого счёта.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHowTo = false }) {
                    Text("Понятно")
                }
            }
        )
    }
}

@Composable
private fun SettingToggle(
    title: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    subtitleColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (enabled) textColor else subtitleColor
        )
        androidx.compose.material3.Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}