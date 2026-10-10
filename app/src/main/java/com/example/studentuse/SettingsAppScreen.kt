package com.example.studentuse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsAppScreen(
    currentThemeMode: Int,
    onThemeModeChange: (Int) -> Unit,
    vibrateOnCount: Boolean,
    onVibrateOnCountChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                text = "Настройки приложения",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // === ТЕМА ===
            Text(
                text = "Тема оформления",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            ThemeOption(
                title = "Системная",
                selected = currentThemeMode == AppSettings.THEME_SYSTEM,
                onClick = { onThemeModeChange(AppSettings.THEME_SYSTEM) }
            )
            Spacer(Modifier.height(8.dp))

            ThemeOption(
                title = "Тёмная",
                selected = currentThemeMode == AppSettings.THEME_DARK,
                onClick = { onThemeModeChange(AppSettings.THEME_DARK) }
            )
            Spacer(Modifier.height(8.dp))

            ThemeOption(
                title = "Светлая",
                selected = currentThemeMode == AppSettings.THEME_LIGHT,
                onClick = { onThemeModeChange(AppSettings.THEME_LIGHT) }
            )

            Spacer(Modifier.height(32.dp))

            // === ВИБРАЦИЯ ПРИ СЧЁТЕ ===
            Text(
                text = "Вибрация",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Вибрация при счёте",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Короткая вибрация при каждом повторе. Помогает в зале, когда шумно.",
                        fontSize = 13.sp,
                        color = subtitleColor
                    )
                }
                Switch(
                    checked = vibrateOnCount,
                    onCheckedChange = onVibrateOnCountChange
                )
            }

            Spacer(Modifier.height(32.dp))

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
        }
    }
}

@Composable
private fun ThemeOption(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected)
                Color(0xFF4CAF50)
            else
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected)
                Color.White
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selected) "✓  $title" else "     $title",
                fontSize = 18.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}