package com.csprout.sobercounter

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
    var showGoals by remember { mutableStateOf(false) }
    var showSponsor by remember { mutableStateOf(false) }
    var sponsorName by remember { mutableStateOf(store.sponsorName) }
    var sponsorPhone by remember { mutableStateOf(store.sponsorPhone) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        GoalScheduler.scheduleAll(context) // re-arm in case alarms were lost (force-stop, update)
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        var menuOpen by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SOBRIETY TRACKER", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic, letterSpacing = 4.sp, modifier = Modifier.weight(1f),
                style = TextStyle(shadow = Shadow(Color.White.copy(alpha = 0.5f), blurRadius = 14f)),
            )
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        painterResource(R.drawable.ic_settings), contentDescription = "Settings",
                        tint = NeonCyan, modifier = Modifier.size(28.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Change sobriety date") },
                        onClick = { menuOpen = false; pickDateTime() },
                    )
                    DropdownMenuItem(
                        text = { Text("Goals") },
                        onClick = { menuOpen = false; showGoals = true },
                    )
                    DropdownMenuItem(
                        text = { Text("Sponsor's number") },
                        onClick = { menuOpen = false; showSponsor = true },
                    )
                }
            }
        }

        val s = start
        if (s == null) {
            Text("Set the date and time your sobriety began.", color = Color(0xFFAAAAAA), fontSize = 16.sp)
            NeonButton("Set sobriety date", NeonGreen, ::pickDateTime)
        } else {
            Text(
                s.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                color = NeonCyan, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                style = TextStyle(shadow = Shadow(NeonCyan, blurRadius = 16f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
            val e = elapsed(s, now)
            NeonBar("YEARS", e.years, e.yearFrac, NeonCyan)
            NeonBar("MONTHS", e.months, e.monthFrac, NeonMagenta)
            NeonBar("DAYS", e.days, e.dayFrac, NeonGreen)
            NeonBar("HOURS", e.hours, e.hourFrac, NeonAmber)

            // Progress runs from the most recently reached goal (or the sobriety date) to the next goal.
            val targets = goals.map { it to it.targetFrom(s) }
            val next = targets.filter { it.second.isAfter(now) }.minByOrNull { it.second }
            if (next != null) {
                val prev = targets.filter { !it.second.isAfter(now) }.maxOfOrNull { it.second } ?: s
                val total = Duration.between(prev, next.second).toMillis().coerceAtLeast(1)
                val done = Duration.between(prev, now).toMillis()
                val left = Duration.between(now, next.second)
                GoalProgress(
                    label = "NEXT: ${next.first.label.uppercase()}",
                    fraction = done.toFloat() / total,
                    detail = "${left.toDays()}d ${left.toHours() % 24}h left",
                )
            }
        }

        Text(
            "RESOURCES", color = Color(0xFF888888), fontSize = 12.sp, fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp, modifier = Modifier.padding(top = 10.dp),
        )
        ResourceChips(
            sponsorName = sponsorName, sponsorPhone = sponsorPhone,
            onSetSponsor = { showSponsor = true },
        )
    }

    if (showSponsor) {
        SponsorDialog(
            name = sponsorName, phone = sponsorPhone,
            onSave = { n, ph ->
                store.sponsorName = n; store.sponsorPhone = ph
                sponsorName = n; sponsorPhone = ph
                showSponsor = false
            },
            onDismiss = { showSponsor = false },
        )
    }

    if (showGoals) {
        GoalsDialog(
            goals = goals, start = start, now = now,
            onAdd = { showAddGoal = true },
            onRemove = { g ->
                GoalScheduler.cancel(context, g)
                store.removeGoal(g.id)
                goals = store.goals
            },
            onDismiss = { showGoals = false },
        )
    }

    if (showAddGoal) {
        AddGoalDialog(
            start = start, now = now, existing = goals,
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
            .padding(horizontal = 18.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                "$value", color = color, fontSize = 36.sp, fontWeight = FontWeight.Light,
                style = TextStyle(shadow = Shadow(color, blurRadius = 24f)),
                modifier = Modifier.weight(1f),
            )
            Text(
                label, color = color.copy(alpha = 0.9f), fontSize = 14.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 5.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth()
                .height(6.dp)
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

/** Wider, brighter rail showing percent of the way from the previous goal to the next one. */
@Composable
private fun GoalProgress(label: String, fraction: Float, detail: String) {
    val color = NeonGreen
    val shape = RoundedCornerShape(20.dp)
    val f = fraction.coerceIn(0f, 1f)
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, color.copy(alpha = 0.6f), shape)
            .background(color.copy(alpha = 0.06f), shape)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                label, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp, modifier = Modifier.weight(1f),
            )
            Text(
                "${(f * 100).toInt()}%", color = color, fontSize = 28.sp, fontWeight = FontWeight.Light,
                style = TextStyle(shadow = Shadow(color, blurRadius = 20f)),
            )
        }
        Box(
            Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(14.dp)
                .drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(color.copy(alpha = 0.14f), size = size, cornerRadius = r)
                    val w = size.width * f
                    if (w > 0f) {
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
        Text(detail, color = Color(0xFF888888), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

private data class ResourceLink(val label: String, val uri: String, val color: Color, val dial: Boolean = false)

private val ResourceLinks = listOf(
    ResourceLink("AA", "https://www.aa.org", NeonCyan),
    ResourceLink("NA", "https://www.na.org", NeonMagenta),
    ResourceLink("SMART Recovery", "https://www.smartrecovery.org", NeonAmber),
    ResourceLink("SAMHSA Helpline", "tel:18006624357", NeonGreen, dial = true),
    ResourceLink("988 Lifeline", "tel:988", NeonCyan, dial = true),
)

/** Opens the browser or dialer. ACTION_DIAL only pre-fills the number; the user presses call. */
private fun open(context: Context, link: ResourceLink) {
    val action = if (link.dial) Intent.ACTION_DIAL else Intent.ACTION_VIEW
    runCatching { context.startActivity(Intent(action, Uri.parse(link.uri))) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceChips(sponsorName: String, sponsorPhone: String, onSetSponsor: () -> Unit) {
    val context = LocalContext.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (sponsorPhone.isBlank()) {
            ResourceChip("+ Add sponsor", NeonGreen, onSetSponsor)
        } else {
            val link = ResourceLink(sponsorName.ifBlank { "Sponsor" }, "tel:" + Uri.encode(sponsorPhone), NeonGreen, dial = true)
            ResourceChip("☎ " + link.label, NeonGreen) { open(context, link) }
        }
        ResourceLinks.forEach { link -> ResourceChip(link.label, link.color) { open(context, link) } }
    }
}

@Composable
private fun ResourceChip(label: String, color: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        modifier = Modifier
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .background(color.copy(alpha = 0.05f), shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun SponsorDialog(name: String, phone: String, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var n by remember { mutableStateOf(name) }
    var ph by remember { mutableStateOf(phone) }
    val context = LocalContext.current
    // The system contact picker hands back a one-time read grant, so no READ_CONTACTS permission is needed.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri = result.data?.data ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    ph = c.getString(0).orEmpty()
                    n = c.getString(1).orEmpty().take(24)
                }
            }
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0B0B),
        title = { Text("Sponsor") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton("Choose from contacts", NeonCyan) {
                    picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI))
                }
                OutlinedTextField(
                    value = n, onValueChange = { n = it.take(24) }, label = { Text("Name (optional)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = ph, onValueChange = { ph = it.filter { c -> c.isDigit() || c in "+-() " }.take(20) },
                    label = { Text("Phone number") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
                Text("Stored only on this phone.", color = Color(0xFF888888), fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(n.trim(), ph.trim()) }) { Text("Save") } },
        dismissButton = {
            Row {
                if (phone.isNotBlank()) TextButton(onClick = { onSave("", "") }) { Text("Remove", color = Color(0xFF888888)) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

/** Typical sobriety milestones, smallest first. */
private val Milestones = listOf(
    1 to GoalUnit.DAYS, 3 to GoalUnit.DAYS, 7 to GoalUnit.DAYS, 14 to GoalUnit.DAYS,
    30 to GoalUnit.DAYS, 60 to GoalUnit.DAYS, 90 to GoalUnit.DAYS,
    6 to GoalUnit.MONTHS, 9 to GoalUnit.MONTHS, 1 to GoalUnit.YEARS, 18 to GoalUnit.MONTHS,
    2 to GoalUnit.YEARS, 3 to GoalUnit.YEARS, 5 to GoalUnit.YEARS, 10 to GoalUnit.YEARS,
)

/** First milestone still in the future that isn't already a goal; falls back to the next whole year. */
private fun suggestGoal(start: LocalDateTime?, now: LocalDateTime, existing: List<Goal>): Pair<Int, GoalUnit> {
    if (start == null) return 30 to GoalUnit.DAYS
    val taken = existing.map { it.targetFrom(start) }.toSet()
    Milestones.firstOrNull { (n, u) ->
        val t = Goal(0, n, u).targetFrom(start)
        t.isAfter(now) && t !in taken
    }?.let { return it }
    return (elapsed(start, now).years.toInt() + 1) to GoalUnit.YEARS
}

@Composable
private fun GoalsDialog(
    goals: List<Goal>, start: LocalDateTime?, now: LocalDateTime,
    onAdd: () -> Unit, onRemove: (Goal) -> Unit, onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0B0B),
        title = { Text("Goals") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (goals.isEmpty()) Text("No goals yet.", color = Color(0xFF888888))
                goals.sortedBy { g -> start?.let { g.targetFrom(it) } }.forEach { g ->
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
                        TextButton(onClick = { onRemove(g) }) { Text("Remove", color = Color(0xFF888888)) }
                    }
                }
                NeonButton("+ Add goal", NeonMagenta, onAdd)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun AddGoalDialog(
    start: LocalDateTime?, now: LocalDateTime, existing: List<Goal>,
    onDismiss: () -> Unit, onAdd: (Int, GoalUnit) -> Unit,
) {
    val suggestion = remember { suggestGoal(start, now, existing) }
    var text by remember { mutableStateOf(suggestion.first.toString()) }
    var unit by remember { mutableStateOf(suggestion.second) }
    val amount = text.toIntOrNull()
    val passed = start != null && amount != null && amount > 0 && !Goal(0, amount, unit).targetFrom(start).isAfter(now)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0B0B0B),
        title = { Text("New goal") },
        text = {
            var menuOpen by remember { mutableStateOf(false) }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(3) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 24.sp, textAlign = TextAlign.Center, color = NeonMagenta),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.size(width = 84.dp, height = 64.dp),
                )
                Box {
                    OutlinedButton(
                        onClick = { menuOpen = true },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonMagenta),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonMagenta),
                        modifier = Modifier.width(130.dp).height(56.dp),
                    ) { Text(unit.label + "  ▾", fontSize = 16.sp) }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        GoalUnit.entries.forEach { u ->
                            DropdownMenuItem(text = { Text(u.label) }, onClick = { unit = u; menuOpen = false })
                        }
                    }
                }
            }
            if (passed) {
                Text("You're already past that. Pick a longer goal.", color = Color(0xFFFF6B6B), fontSize = 13.sp)
            }
            }
        },
        confirmButton = {
            TextButton(onClick = { amount?.let { onAdd(it, unit) } }, enabled = amount != null && amount > 0 && !passed) {
                Text("Add")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
