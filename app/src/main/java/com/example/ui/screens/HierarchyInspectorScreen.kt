package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UiNode
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.WarningAmber

@Composable
fun HierarchyInspectorScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val hierarchy by viewModel.currentHierarchy.collectAsState()
    val searchQuery by viewModel.nodeSearchQuery.collectAsState()
    val foregroundPackage by viewModel.foregroundPackage.collectAsState()
    val activityClass by viewModel.activityClass.collectAsState()

    // Flatten tree for list search
    val flattenedNodes = remember(hierarchy, searchQuery) {
        val list = mutableListOf<UiNode>()
        fun collect(node: UiNode) {
            val matches = searchQuery.isBlank() ||
                    (node.text?.contains(searchQuery, ignoreCase = true) == true) ||
                    (node.viewIdResourceName?.contains(searchQuery, ignoreCase = true) == true) ||
                    (node.contentDescription?.contains(searchQuery, ignoreCase = true) == true) ||
                    (node.className?.contains(searchQuery, ignoreCase = true) == true)

            if (matches) list.add(node)
            node.children.forEach { collect(it) }
        }
        hierarchy?.let { collect(it) }
        list
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // App / Window Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ACTIVE WINDOW INSPECTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Text(
                    text = foregroundPackage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            IconButton(
                onClick = { viewModel.refreshHierarchy() },
                modifier = Modifier.testTag("refresh_hierarchy_button")
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Hierarchy", tint = CyberCyan)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setNodeSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("hierarchy_search_input"),
            placeholder = { Text("Filter by viewId, text, description or class...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Metrics Banner
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricChip(label = "Visible Nodes", value = "${flattenedNodes.size}", modifier = Modifier.weight(1f))
            MetricChip(label = "Clickable", value = "${flattenedNodes.count { it.isClickable }}", modifier = Modifier.weight(1f))
            MetricChip(label = "Editable", value = "${flattenedNodes.count { it.isEditable }}", modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (flattenedNodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No UI nodes matched. Pull to refresh or switch to target app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(flattenedNodes) { node ->
                    NodeInspectorCard(node = node, onTestTap = { viewModel.testTapNode(node) })
                }
            }
        }
    }
}

@Composable
fun MetricChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CyberCyan)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
    }
}

@Composable
fun NodeInspectorCard(node: UiNode, onTestTap: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = node.displayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (node.isClickable) CyberCyan else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (node.isClickable) {
                        TagChip(label = "CLICKABLE", color = NeonEmerald)
                    }
                    if (node.isEditable) {
                        TagChip(label = "EDITABLE", color = CyberCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            node.viewIdResourceName?.let {
                Text(
                    text = "ID: $it",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Text(
                text = "Class: ${node.className?.substringAfterLast(".") ?: "Unknown"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Coordinates & Test Trigger
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Bounds: [${node.bounds.left},${node.bounds.top}][${node.bounds.right},${node.bounds.bottom}] Center: (${node.centerX.toInt()}, ${node.centerY.toInt()})",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )

                OutlinedButton(
                    onClick = onTestTap,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("test_tap_node_${node.id.take(6)}")
                ) {
                    Icon(Icons.Default.AdsClick, contentDescription = null, modifier = Modifier.size(14.dp), tint = CyberCyan)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trigger Tap", fontSize = 11.sp, color = CyberCyan)
                }
            }
        }
    }
}

@Composable
fun TagChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
