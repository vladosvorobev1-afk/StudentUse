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
fun PlankSettingsScreen(
    targetSeconds: Int,
    onTargetSecondsChange: (Int) -> Unit,
    saySetComplete: Boolean,
    onSaySetCompleteChange: (Boolean) -> Unit,
    vibrateOnSetComplete: Boolean,
    onVibrateOnSetCompleteChange: (Boolean) -> Unit,
    sayBodyLine: Boolean,
    onSayBodyLineChange: (Boolean) -> Unit,
    saySeconds: Boolean,
    onSaySecondsChange: (Boolean) -> Unit,
    sayGood: Boolean,
    onSayGoodChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val noTarget = targetSeconds == AppSettings.TARGET_SECONDS_NONE
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
                text = "Настройки планки",
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

            // === ЦЕЛЬ ===
            Text(
                text = "Цель в секундах",
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
                    val next = (targetSeconds - AppSettings.TARGET_SECONDS_STEP)
                        .coerceAtLeast(AppSettings.TARGET_SECONDS_MIN)
                    onTargetSecondsChange(next)
                }) {
                    Text("−", fontSize = 20.sp)
                }

                Text(
                    text = if (noTarget) "Без цели" else targetSeconds.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.width(100.dp)
                )

                Button(onClick = {
                    val next = (targetSeconds + AppSettings.TARGET_SECONDS_STEP)
                        .coerceAtMost(AppSettings.TARGET_SECONDS_MAX)
                    onTargetSecondsChange(next)
                }) {
                    Text("+", fontSize = 20.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    onTargetSecondsChange(AppSettings.TARGET_SECONDS_NONE)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (noTarget)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (noTarget)
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
                enabled = !noTarget,
                onCheckedChange = onSaySetCompleteChange,
                textColor = textColor
            )

            Spacer(Modifier.height(12.dp))

            SettingToggle(
                title = "Вибрация для тренера/партнёра",
                checked = vibrateOnSetComplete,
                onCheckedChange = onVibrateOnSetCompleteChange,
                textColor = textColor
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
                title = "«Держи тело ровно»",
                checked = sayBodyLine,
                onCheckedChange = onSayBodyLineChange,
                textColor = textColor
            )

            Spacer(Modifier.height(12.dp))

            SettingToggle(
                title = "Счёт секунд (каждые 10 сек)",
                checked = saySeconds,
                onCheckedChange = onSaySecondsChange,
                textColor = textColor
            )

            Spacer(Modifier.height(12.dp))

            SettingToggle(
                title = "«Хорошо»",
                checked = sayGood,
                onCheckedChange = onSayGoodChange,
                textColor = textColor
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
                        "2. Встань БОКОМ к камере — тело в профиль.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "3. Встань в планку на прямых руках, чтобы тело было видно целиком.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "4. Держи планку — таймер идёт, пока линия тела ровная.",
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "📱 ВАЖНО: камера должна быть на высоте 50–60 см. Если поставить на пол — линия тела не видна, таймер не идёт.",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "💡 Если таймер не идёт — проверь ракурс: камера должна видеть тебя БОКОМ.",
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
    textColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            color = if (enabled) textColor else MaterialTheme.colorScheme.onSurfaceVariant
        )
        androidx.compose.material3.Switch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}