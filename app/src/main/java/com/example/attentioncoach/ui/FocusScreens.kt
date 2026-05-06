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
import com.example.attentioncoach.domain.PlannedTask

@Composable
fun WorkScreen(
    task: PlannedTask,
    onPause: () -> Unit,
    onExit: () -> Unit,
    onNeededAppSelected: (String) -> Unit
) {
    var confirmExit by remember { mutableStateOf(false) }
    var neededMenuOpen by remember { mutableStateOf(false) }

    BackHandler { confirmExit = true }

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
                        Text("18:42", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                        Text("ELAPSED", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                        listOf("com.android.chrome" to "Chrome", "com.google.android.apps.docs" to "Docs").forEach { app ->
                            DropdownMenuItem(
                                text = { Text(app.second) },
                                onClick = {
                                    neededMenuOpen = false
                                    onNeededAppSelected(app.first)
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
                        onClick = { confirmExit = true },
                        modifier = Modifier.weight(1f).height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = UiTokens.RedChipText)
                    ) {
                        Text("Exit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Leave this focus block?", fontWeight = FontWeight.Bold) },
            text = { Text("Exiting will stop the current timer and return to the main page. Your original plan will stay unchanged.") },
            confirmButton = {
                Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = UiTokens.RedChipText)) {
                    Text("Continue")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmExit = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PauseScreen(onResume: () -> Unit) {
    BackHandler { }
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
                Text("02:53", fontSize = 58.sp, fontWeight = FontWeight.Bold)
                Text("This is the pause limit. Keep it short so the focus block stays recoverable.", textAlign = TextAlign.Center, color = UiTokens.InkSoft)
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(58.dp)) {
                    Text("Continue focus", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReentryScreen(
    task: PlannedTask,
    onResume: () -> Unit,
    onAdjustPlan: () -> Unit,
    onRecordReason: () -> Unit
) {
    BackHandler { onResume() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(UiTokens.Page)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Re-entry", color = UiTokens.InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(task.title, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    "You planned this block. Want to return, adjust the plan, or record why now is not right?",
                    color = UiTokens.InkSoft,
                    textAlign = TextAlign.Center
                )
                Button(onClick = onResume, modifier = Modifier.fillMaxWidth()) {
                    Text("Resume task")
                }
                OutlinedButton(onClick = onAdjustPlan, modifier = Modifier.fillMaxWidth()) {
                    Text("Adjust plan")
                }
                OutlinedButton(onClick = onRecordReason, modifier = Modifier.fillMaxWidth()) {
                    Text("Record reason")
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
