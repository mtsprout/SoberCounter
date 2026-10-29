package com.csprout.sobercounter

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val NeonCyan = Color(0xFF00E5FF)
private val NeonMagenta = Color(0xFFFF2BD6)
private val NeonGreen = Color(0xFF39FF14)
private val NeonAmber = Color(0xFFFFB300)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color.Black, surface = Color.Black, primary = NeonCyan,
                    onBackground = Color.White, onSurface = Color.White,
                )
            ) { SoberApp() }
        }
    }
}

@Composable
private fun SoberApp() {
    val context = LocalContext.current
    val store = remember { Store(context) }
    var start by remember { mutableStateOf(store.soberStart) }
    var goals by remember { mutableStateOf(store.goals) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var showAddGoal by remember { mutableStateOf(false) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        while (true) {
            now = LocalDateTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    fun setStart(value: LocalDateTime) {
        store.soberStart = value
        start = value
        GoalScheduler.scheduleAll(context)
    }

    fun pickDateTime() {
        val base = start ?: LocalDateTime.now()
        DatePickerDialog(context, { _, y, m, d ->
            TimePickerDialog(context, { _, hh, mm ->
                setStart(LocalDateTime.of(y, m + 1, d, hh, mm))
            }, base.hour, base.minute, false).show()
        }, base.year, base.monthValue - 1, base.dayOfMonth).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "SOBER", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
        )

        val s = start
        if (s == null) {
            Text("Set the date and time your sobriety began.", color = Color(0xFFAAAAAA), fontSize = 16.sp)
            NeonButton("Set sobriety date", NeonGreen, ::pickDateTime)
        } else {
            val e = elapsed(s, now)
            NeonBar("YEARS", e.years, e.yearFrac, NeonCyan)
            NeonBar("MONTHS", e.months, e.monthFrac, NeonMagenta)
            NeonBar("DAYS", e.days, e.dayFrac, NeonGreen)
            NeonBar("HOURS", e.hours, e.hourFrac, NeonAmber)

            Text(
                "Since " + s.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                color = Color(0xFF888888), fontSize = 13.sp,
            )
            NeonButton("Change date / time", NeonCyan, ::pickDateTime)

            val next = goals.map { it to it.targetFrom(s) }.filter { it.second.isAfter(now) }.minByOrNull { it.second }
            if (next != null) {
                val left = Duration.between(now, next.second)
                Text(
                    "Next goal: ${next.first.label} — ${left.toDays()}d ${left.toHours() % 24}h to go",
                    color = NeonGreen, fontSize = 15.sp,
                    style = TextStyle(shadow = Shadow(NeonGreen, blurRadius = 12f)),
                )
            }
        }

        Text(
            "GOALS", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp, modifier = Modifier.padding(top = 12.dp),
        )
        goals.sortedBy { start?.let { s -> it.targetFrom(s) } }.forEach { g ->
            val reached = start?.let { !g.targetFrom(it).isAfter(now) } == true
            Row(
                Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(14.dp))
                    .padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    (if (reached) "✓  " else "") + g.label,
                    color = if (reached) NeonGreen else Color.White,
                    fontSize = 17.sp, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = {
                    GoalScheduler.cancel(context, g)
                    store.removeGoal(g.id)
                    goals = store.goals
                }) { Text("Remove", color = Color(0xFF888888)) }
            }
        }
        NeonButton("+ Add goal", NeonMagenta) { showAddGoal = true }
    }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onAdd = { amount, unit ->
                store.addGoal(amount, unit)
                goals = store.goals
                GoalScheduler.scheduleAll(context)
                showAddGoal = false
            },
        )
    }
}

@Composable
private fun NeonButton(text: String, color: Color, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color),
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

/** A streamlined neon bar: big glowing number, spaced label, and a slim glowing progress rail. */
@Composable
private fun NeonBar(label: String, value: Long, fraction: Float, color: Color) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .background(color.copy(alpha = 0.04f), shape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                "$value", color = color, fontSize = 52.sp, fontWeight = FontWeight.Light,
                style = TextStyle(shadow = Shadow(color, blurRadius = 24f)),
                modifier = Modifier.weight(1f),
            )
            Text(
                label, color = color.copy(alpha = 0.9f), fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 5.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
        Box(
            Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(8.dp)
                .drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(color.copy(alpha = 0.14f), size = size, cornerRadius = r)
                    val w = size.width * fraction.coerceIn(0f, 1f)
                    if (w > 0f) {
                        // Soft glow: a few wider, fainter passes behind the bar.
                        for (i in 3 downTo 1) {
                            val pad = i * 3.5f
                            drawRoundRect(
                                color.copy(alpha = 0.07f),
                                topLeft = Offset(-pad, -pad),
                                size = Size(w + pad * 2, size.height + pad * 2),
                                cornerRadius = CornerRadius(size.height / 2 + pad),
                            )
                        }
                        drawRoundRect(
                            Brush.horizontalGradient(listOf(color.copy(alpha = 0.55f), color)),
                            size = Size(w.coerceAtLeast(size.height), size.height),
                            cornerRadius = r,
                        )
                    }
                }
        )
    }
}

@Composable
private fun AddGoalDialog(onDismiss: () -> Unit, onAdd: (Int, GoalUnit) -> Unit) {
    var text by remember { mutableStateOf("30") }
    var unit by remember { mutableStateOf(GoalUnit.DAYS) }
    val amount = text.toIntOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0B0B),
        title = { Text("New goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(4) },
                    label = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GoalUnit.entries.forEach { u ->
                        FilterChip(
                            selected = unit == u, onClick = { unit = u },
                            label = { Text(u.label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonMagenta.copy(alpha = 0.25f),
                            ),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { amount?.let { onAdd(it, unit) } }, enabled = amount != null && amount > 0) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
