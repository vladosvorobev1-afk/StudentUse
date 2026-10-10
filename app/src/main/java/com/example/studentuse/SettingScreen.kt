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
import kotlin.math.max
import kotlin.math.min

@Composable
fun SettingsScreen(
    targetReps: Int,
    onTargetRepsChange: (Int) -> Unit,
    saySetComplete: Boolean,
    onSaySetCompleteChange: (Boolean) -> Unit,
    vibrateOnSetComplete: Boolean,
    onVibrateOnSetCompleteChange: (Boolean) -> Unit,
    sayBack: Boolean,
    onSayBackChange: (Boolean) -> Unit,
    sayHeels: Boolean,
    onSayHeelsChange: (Boolean) -> Unit,
    sayDeeper: Boolean,
    onSayDeeperChange: (Boolean) -> Unit,
    sayGood: Boolean,
    onSayGoodChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noTarget = targetReps == AppSettings.TARGET_REPS_NONE
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Настройки приседаний",
                color = textColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (noTarget) {
                        "Цель за подход: без цели"
                    } else {
                        "Цель за подход: $targetReps повторов"
                    },
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            val current = if (noTarget) AppSettings.DEFAULT_TARGET_REPS else targetReps
                            val newValue = max(
                                AppSettings.TARGET_REPS_MIN,
                                current - AppSettings.TARGET_REPS_STEP
                            )
                            onTargetRepsChange(newValue)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("−", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = if (noTarget) "∞" else targetReps.toString(),
                        color = textColor,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = {
                            val current = if (noTarget) AppSettings.DEFAULT_TARGET_REPS else targetReps
                            val newValue = min(
                                AppSettings.TARGET_REPS_MAX,
                                current + AppSettings.TARGET_REPS_STEP
                            )
                            onTargetRepsChange(newValue)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("+", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (noTarget) {
                            onTargetRepsChange(AppSettings.DEFAULT_TARGET_REPS)
                        } else {
                            onTargetRepsChange(AppSettings.TARGET_REPS_NONE)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (noTarget)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (noTarget)
                            Color.White
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(
                        text = if (noTarget) "Без цели (включено)" else "Без цели",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            SettingToggle(
                title = "Говорить «Подход завершён»",
                subtitle = if (noTarget) "Не работает при «Без цели»"
                else "Когда достигнуто целевое количество повторов",
                value = saySetComplete,
                onValueChange = onSaySetCompleteChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            SettingToggle(
                title = "Вибрация при завершении подхода",
                subtitle = if (noTarget) "Не работает при «Без цели»"
                else "Короткая вибрация после фразы",
                value = vibrateOnSetComplete,
                onValueChange = onVibrateOnSetCompleteChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Голосовые подсказки",
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "«Держи спину»",
                subtitle = "Если наклоняешь корпус вперёд",
                value = sayBack,
                onValueChange = onSayBackChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "«Пятки на полу»",
                subtitle = "Если отрываешь пятки при приседе",
                value = sayHeels,
                onValueChange = onSayHeelsChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "«Глубже»",
                subtitle = "Если не дошёл до параллели",
                value = sayDeeper,
                onValueChange = onSayDeeperChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingToggle(
                title = "«Хорошо»",
                subtitle = "Похвала за правильный повтор",
                value = sayGood,
                onValueChange = onSayGoodChange,
                textColor = textColor,
                subtitleColor = subtitleColor
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Назад", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))
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
                        "2. Встань ПРЯМО (спереди) к камере — тело анфас, обе ноги видны симметрично.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "3. Встань так, чтобы камера видела тебя в полный рост — от головы до стоп.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "4. Делай приседания.",
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
                        text = "💡 Если счёт не идёт — проверь ракурс: камера должна видеть тебя ПРЯМО (спереди).",
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
    subtitle: String,
    value: Boolean,
    onValueChange: (Boolean) -> Unit,
    textColor: Color,
    subtitleColor: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            color = textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subtitle,
            color = subtitleColor,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Button(
                onClick = { onValueChange(true) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (value) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (value) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Вкл", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { onValueChange(false) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!value) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (!value) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Выкл", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}