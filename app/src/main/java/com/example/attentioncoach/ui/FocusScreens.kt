package com.example.attentioncoach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attentioncoach.domain.ActiveWork
import com.example.attentioncoach.domain.NeededApp
import com.example.attentioncoach.domain.PlannedTask
import com.example.attentioncoach.domain.WorkSessionClock
import kotlinx.coroutines.delay

@Composable
fun WorkScreen(
    task: PlannedTask,
    activeWork: ActiveWork,
    onPause: () -> Unit,
    onFinish: () -> Unit,
    onExit: () -> Unit,
    neededApps: List<NeededApp>,
    onNeededAppSelected: (String) -> Unit
) {
    var showExitConfirm by remember { mutableStateOf(false) }
    var showFinishConfirm by remember { mutableStateOf(false) }
    var neededMenuOpen by remember { mutableStateOf(false) }
    var nowMillis by remember(activeWork.taskId, activeWork.startedAtMillis) { mutableStateOf(System.currentTimeMillis()) }
    val activeMillis = WorkSessionClock.activeMillisAt(activeWork, nowMillis)
    val timerText = WorkSessionClock.workTimerText(task.durationMinutes, activeMillis)
    val timerLabel = if (timerText.startsWith("+")) "OVERTIME" else "REMAINING"

    LaunchedEffect(activeWork.taskId, activeWork.startedAtMillis, activeWork.accumulatedActiveMillis) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }

    BackHandler { showExitConfirm = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UiTokens.Page)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("FOCUS BLOCK", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(task.title, fontSize = 30.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FocusChip("${task.durationMinutes} min")
                    FocusChip(task.priority.displayName())
                }
            }

            Box(
                modifier = Modifier
                    .size(236.dp)
                    .clip(CircleShape)
                    .background(UiTokens.GoogleBlue),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(196.dp)
                        .clip(CircleShape)
                        .background(androidx.compose.ui.graphics.Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(timerText, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Text(timerLabel, color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box {
                    OutlinedButton(
                        onClick = { neededMenuOpen = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Needed apps", fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = neededMenuOpen, onDismissRequest = { neededMenuOpen = false }) {
                        neededApps.forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.label) },
                                onClick = {
                                    neededMenuOpen = false
                                    onNeededAppSelected(app.packageName)
                                }
                            )
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(54.dp)) {
                        Text("Pause", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showFinishConfirm = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleGreen)
                    ) {
                        Text("Finish", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { showExitConfirm = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UiTokens.RedChipText)
                    ) {
                        Text("Exit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showFinishConfirm) {
        AlertDialog(
            onDismissRequest = { showFinishConfirm = false },
            title = {
                Text(
                    "Finish this task?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This will stop the timer and record your actual focus time.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showFinishConfirm = false
                        onFinish()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.GoogleGreen)
                ) {
                    Text("Finish")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showFinishConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = {
                Text(
                    "Exit focus block?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("This stops the current timer without changing the plan or recording focus time.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirm = false
                        onExit()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = UiTokens.RedChipText)
                ) {
                    Text("Exit")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showExitConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PauseScreen(activeWork: ActiveWork, onResume: () -> Unit) {
    BackHandler { }
    val pauseStartedAt = activeWork.pauseStartedAtMillis ?: System.currentTimeMillis()
    var nowMillis by remember(pauseStartedAt) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(pauseStartedAt) {
        while (true) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(UiTokens.Page)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
            shape = RoundedCornerShape(30.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.padding(30.dp)
            ) {
                Text("Pause ends before it steals the block.", textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                Text(WorkSessionClock.pauseTimerText(pauseStartedAt, nowMillis), fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text("This is the pause limit. Keep it short so the focus block stays recoverable.", textAlign = TextAlign.Center, color = UiTokens.InkSoft)
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                    Text("Continue focus", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FocusChip(text: String) {
    Text(
        text = text,
        color = UiTokens.ImportantChipText,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(UiTokens.ImportantChipBg)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    )
}
