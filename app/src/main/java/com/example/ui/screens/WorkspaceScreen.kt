package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AgentWorkflow
import com.example.model.ExecutionTier
import com.example.model.StepStatus
import com.example.model.WorkflowStep
import com.example.ui.MainViewModel
import com.example.ui.theme.CriticalRed
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun WorkspaceScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val isServiceConnected by viewModel.isServiceConnected.collectAsState()
    val isAgentRunning by viewModel.isAgentRunning.collectAsState()
    val activeWorkflow by viewModel.activeWorkflow.collectAsState()
    val currentStepIndex by viewModel.currentStepIndex.collectAsState()
    val isSystemAlertDetected by viewModel.isSystemAlertDetected.collectAsState()
    val lastAlertMessage by viewModel.lastAlertMessage.collectAsState()
    val foregroundPackage by viewModel.foregroundPackage.collectAsState()
    val workflowLibrary by viewModel.workflowLibrary.collectAsState()
    val actionHistory by viewModel.actionHistory.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Accessibility Service Authorization Status
        item {
            ServiceStatusBanner(
                isServiceConnected = isServiceConnected,
                foregroundPackage = foregroundPackage,
                onEnableClick = { viewModel.openAccessibilitySettings(context) }
            )
        }

        // 2. Intercepted System Dialog / Permission Alert Bar
        item {
            AnimatedVisibility(visible = isSystemAlertDetected) {
                SystemAlertInterventionCard(
                    message = lastAlertMessage ?: "System Dialog Detected",
                    onAutoResolve = { viewModel.autoResolveSystemDialog() }
                )
            }
        }

        // 3. Workflow Selector Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AUTONOMOUS AGENT RECIPES",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(workflowLibrary) { wf ->
                        WorkflowSelectorCard(
                            workflow = wf,
                            isSelected = activeWorkflow?.id == wf.id,
                            isEnabled = !isAgentRunning,
                            onSelect = { viewModel.selectWorkflow(wf) }
                        )
                    }
                }
            }
        }

        // 4. Active Workflow Control Card
        item {
            activeWorkflow?.let { wf ->
                ActiveExecutionCard(
                    workflow = wf,
                    isRunning = isAgentRunning,
                    currentStepIndex = currentStepIndex,
                    onStart = { viewModel.startAgent() },
                    onStop = { viewModel.stopAgent() }
                )
            }
        }

        // 5. Live Step Pipeline Timeline
        item {
            Text(
                text = "EXECUTION PIPELINE & RESILIENCE MATRIX",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        }

        activeWorkflow?.let { wf ->
            items(wf.steps) { step ->
                StepExecutionRow(step = step, isRunning = isAgentRunning && wf.steps.indexOf(step) == currentStepIndex)
            }
        }

        // 6. Recent Action Audit Feed
        if (actionHistory.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FALLBACK AUDIT LOG (LAST 10 ACTIONS)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            items(actionHistory.take(8)) { action ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = action.message,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Latency: ${action.latencyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TierBadge(tier = action.tierUsed)
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceStatusBanner(
    isServiceConnected: Boolean,
    foregroundPackage: String,
    onEnableClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isServiceConnected)
                MaterialTheme.colorScheme.surface
            else
                WarningAmber.copy(alpha = 0.12f)
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isServiceConnected) NeonEmerald.copy(alpha = 0.4f) else WarningAmber,
                RoundedCornerShape(14.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isServiceConnected) NeonEmerald else WarningAmber)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceConnected) "ACCESSIBILITY ENGINE ACTIVE" else "ACCESSIBILITY PERMISSION REQUIRED",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceConnected) NeonEmerald else WarningAmber
                    )
                }

                if (!isServiceConnected) {
                    Button(
                        onClick = onEnableClick,
                        colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("enable_accessibility_button")
                    ) {
                        Text("Enable Service", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isServiceConnected)
                    "Inspecting foreground app: $foregroundPackage • Non-rooted RAUSON node active."
                else
                    "Grant Mobile Croaker Accessibility Service permission to enable autonomous cross-app UI navigation & gesture dispatch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Inspired by project OpenClaw (github.com/openclaw/openclaw) • Created by Inceptorium Apokryf (apokryf.pl)",
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SystemAlertInterventionCard(
    message: String,
    onAutoResolve: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CriticalRed.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CriticalRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = CriticalRed)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "System UI Dialog Intercepted",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = CriticalRed
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
            }
            Button(
                onClick = onAutoResolve,
                colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("auto_resolve_button")
            ) {
                Text("Auto-Resolve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WorkflowSelectorCard(
    workflow: AgentWorkflow,
    isSelected: Boolean,
    isEnabled: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else
                MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .width(220.dp)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = isEnabled, onClick = onSelect)
            .testTag("workflow_${workflow.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = workflow.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) CyberCyan else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${workflow.steps.size} steps • ${workflow.targetPackage.substringAfterLast(".")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = workflow.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun ActiveExecutionCard(
    workflow: AgentWorkflow,
    isRunning: Boolean,
    currentStepIndex: Int,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = workflow.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Target: ${workflow.targetPackage}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                }

                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = CyberCyan
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress bar
            val progress = if (workflow.steps.isNotEmpty()) {
                (currentStepIndex.toFloat() / workflow.steps.size.toFloat()).coerceIn(0f, 1f)
            } else 0f
            LinearProgressIndicator(
                progress = { if (isRunning) progress else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = CyberCyan,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Control Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!isRunning) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("start_agent_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Engage Autonomous Agent", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onStop,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("stop_agent_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CriticalRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Kill-Switch", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StepExecutionRow(step: WorkflowStep, isRunning: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (step.status) {
                StepStatus.RUNNING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                StepStatus.COMPLETED_PRIMARY -> NeonEmerald.copy(alpha = 0.08f)
                StepStatus.COMPLETED_FALLBACK -> WarningAmber.copy(alpha = 0.08f)
                StepStatus.FAILED -> CriticalRed.copy(alpha = 0.08f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                when (step.status) {
                    StepStatus.RUNNING -> CyberCyan
                    StepStatus.COMPLETED_PRIMARY -> NeonEmerald.copy(alpha = 0.5f)
                    StepStatus.COMPLETED_FALLBACK -> WarningAmber.copy(alpha = 0.5f)
                    StepStatus.FAILED -> CriticalRed.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                },
                RoundedCornerShape(10.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                when (step.status) {
                    StepStatus.RUNNING -> CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = CyberCyan
                    )
                    StepStatus.COMPLETED_PRIMARY -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                    StepStatus.COMPLETED_FALLBACK -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                    StepStatus.FAILED -> Icon(Icons.Default.Error, contentDescription = null, tint = CriticalRed, modifier = Modifier.size(18.dp))
                    StepStatus.IDLE -> Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = step.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Type: ${step.actionType.name}" + (step.targetText?.let { " • Target: \"$it\"" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (step.errorDetails != null) {
                        Text(
                            text = "Error: ${step.errorDetails}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CriticalRed
                        )
                    }
                }
            }

            if (step.tierUsed != null) {
                TierBadge(tier = step.tierUsed!!)
            }
        }
    }
}

@Composable
fun TierBadge(tier: ExecutionTier) {
    val (bgColor, textColor) = when (tier) {
        ExecutionTier.ACCESSIBILITY_NODE -> Pair(NeonEmerald.copy(alpha = 0.2f), NeonEmerald)
        ExecutionTier.ACCESSIBILITY_GESTURE -> Pair(CyberCyan.copy(alpha = 0.2f), CyberCyan)
        ExecutionTier.ADB_BRIDGE -> Pair(WarningAmber.copy(alpha = 0.2f), WarningAmber)
        ExecutionTier.CV_HEURISTIC -> Pair(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), MaterialTheme.colorScheme.secondary)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = tier.label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 10.sp
        )
    }
}
