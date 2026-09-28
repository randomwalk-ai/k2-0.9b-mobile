package com.example.llama.aichat.ui

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.llama.aichat.ai.K2InferenceManager
import com.example.llama.aichat.ai.RuleClassifier
import com.example.llama.aichat.ai.RuleIntent
import com.example.llama.aichat.data.NotificationRecord
import com.example.llama.aichat.data.NotificationRule
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestNotificationPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val importantNotifications by viewModel.importantNotifications.collectAsState()
    val unimportantNotifications by viewModel.unimportantNotifications.collectAsState()
    val allNotifications by viewModel.allNotifications.collectAsState()
    val retentionPeriod by viewModel.retentionPeriod.collectAsState()
    val rules by viewModel.rules.collectAsState(initial = emptyList())
    val isAccessEnabled by viewModel.isNotificationAccessEnabled.collectAsState()
    val isPermissionGranted by viewModel.isNotificationPermissionGranted.collectAsState()
    val isEnabled by viewModel.isEnabled.collectAsState()
    val modelState by viewModel.modelState.collectAsState()
    val isAiAlertSoundEnabled by viewModel.isAiAlertSoundEnabled.collectAsState()
    val selectedNotification by viewModel.selectedNotification.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()

    var showAddRuleDialog by remember { mutableStateOf(false) }
    var showRetentionDialog by remember { mutableStateOf(false) }
    var initialRuleSuggestion by remember { mutableStateOf("") }
    var ruleToEdit by remember { mutableStateOf<NotificationRule?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let { viewModel.importModel(it) }
    }

    // Filter notifications based on activeFilter and searchQuery
    val filteredNotifications = remember(allNotifications, importantNotifications, unimportantNotifications, activeFilter, searchQuery) {
        val baseList = when (activeFilter) {
            "IMPORTANT" -> importantNotifications
            "NOT_IMPORTANT" -> unimportantNotifications
            else -> allNotifications
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val q = searchQuery.trim().lowercase()
            baseList.filter {
                (it.appName.lowercase().contains(q)) ||
                (it.sender?.lowercase()?.contains(q) == true) ||
                (it.title?.lowercase()?.contains(q) == true) ||
                (it.summary.lowercase().contains(q)) ||
                (it.text?.lowercase()?.contains(q) == true) ||
                (it.aiCategory.lowercase().contains(q))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NOTIFICATION ANALYZER", fontWeight = FontWeight.Bold) },
                actions = {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                StatusSection(
                    isAccessEnabled = isAccessEnabled,
                    isPermissionGranted = isPermissionGranted,
                    modelState = modelState,
                    onEnableClick = {
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    },
                    onRequestPermissionClick = onRequestNotificationPermission,
                    onRetryClick = {
                        viewModel.retryModelLoad()
                    },
                    onPickFileClick = {
                        filePickerLauncher.launch("*/*")
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))

                RulesSection(
                    rules = rules,
                    onAddClick = {
                        initialRuleSuggestion = ""
                        showAddRuleDialog = true
                    },
                    onToggle = { viewModel.toggleRule(it) },
                    onEdit = { ruleToEdit = it },
                    onDelete = { viewModel.deleteRule(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))

                RetentionSection(
                    selectedPeriod = retentionPeriod,
                    onClick = { showRetentionDialog = true }
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Search & Filter Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("NOTIFICATIONS (${retentionPeriod.label})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "${filteredNotifications.size} item${if (filteredNotifications.size != 1) "s" else ""}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search sender, text, app...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear search", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = activeFilter == "ALL",
                        onClick = { viewModel.setActiveFilter("ALL") },
                        label = { Text("All (${allNotifications.size})") }
                    )
                    FilterChip(
                        selected = activeFilter == "IMPORTANT",
                        onClick = { viewModel.setActiveFilter("IMPORTANT") },
                        label = { Text("⚡ Important (${importantNotifications.size})") }
                    )
                    FilterChip(
                        selected = activeFilter == "NOT_IMPORTANT",
                        onClick = { viewModel.setActiveFilter("NOT_IMPORTANT") },
                        label = { Text("Other (${unimportantNotifications.size})") }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            if (filteredNotifications.isEmpty()) {
                item {
                    Text(
                        if (searchQuery.isNotBlank()) "No notifications match '$searchQuery'" else "No notifications analyzed yet.",
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            } else {
                items(filteredNotifications, key = { it.id }) { record ->
                    NotificationItem(
                        record = record,
                        isImportant = record.important,
                        onClick = { viewModel.selectNotification(record) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AI ALERT SOUND", fontWeight = FontWeight.Bold)
                            Text(
                                if (isAiAlertSoundEnabled) "Enabled" else "Disabled",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        Switch(
                            checked = isAiAlertSoundEnabled,
                            onCheckedChange = { viewModel.toggleAiAlertSound() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All History")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        "Dual-Engine Personal AI Architecture:\n• Fast-Path Engine (<1ms) executes direct contact rules instantly without waking RAM.\n• K2 Horizon 0.9B On-Device AI resolves semantic, conditional, and multi-rule intersections on device.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            initialText = initialRuleSuggestion,
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { text ->
                viewModel.addRule(text)
                showAddRuleDialog = false
            }
        )
    }

    ruleToEdit?.let { rule ->
        EditRuleDialog(
            rule = rule,
            onDismiss = { ruleToEdit = null },
            onConfirm = { newText ->
                viewModel.updateRule(rule, newText)
                ruleToEdit = null
            }
        )
    }

    if (showRetentionDialog) {
        RetentionDialog(
            currentPeriod = retentionPeriod,
            onDismiss = { showRetentionDialog = false },
            onSelect = { period ->
                viewModel.setRetentionPeriod(period)
                showRetentionDialog = false
            }
        )
    }

    selectedNotification?.let { record ->
        NotificationDetailDialog(
            record = record,
            onDismiss = { viewModel.selectNotification(null) },
            onDelete = { viewModel.deleteNotification(record) },
            onAddRuleForSender = { sender ->
                initialRuleSuggestion = "Urgent messages from $sender are important."
                showAddRuleDialog = true
            }
        )
    }
}

@Composable
fun StatusSection(
    isAccessEnabled: Boolean,
    isPermissionGranted: Boolean,
    modelState: K2InferenceManager.State,
    onEnableClick: () -> Unit,
    onRequestPermissionClick: () -> Unit,
    onRetryClick: () -> Unit,
    onPickFileClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Notification Access: ", fontWeight = FontWeight.Medium)
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isAccessEnabled) Color(0xFF4CAF50) else Color.Red, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isAccessEnabled) "Connected" else "Not connected",
                    color = if (isAccessEnabled) Color(0xFF4CAF50) else Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            if (!isAccessEnabled) {
                Button(onClick = onEnableClick, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Enable Notification Access")
                }
            }

            if (!isPermissionGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Alert Notifications: ", fontWeight = FontWeight.Medium)
                    Text("Permission required", color = Color.Red, fontSize = 12.sp)
                }
                Button(
                    onClick = onRequestPermissionClick,
                    modifier = Modifier.padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Grant Notification Permission")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("K2 Horizon 0.9B Q4 + Fast Engine", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("On-device · 0% Cloud", fontSize = 11.sp, color = Color.Gray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (modelState == K2InferenceManager.State.ERROR) {
                        OutlinedButton(
                            onClick = onRetryClick,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Retry", fontSize = 12.sp)
                        }
                    }
                    FilledTonalButton(
                        onClick = onPickFileClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Import", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (modelState == K2InferenceManager.State.ERROR) {
                Text(
                    "Error: The model file might be corrupted. Please ensure you have the full 600MB .gguf file and try importing again.",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            val (stateText, stateColor, stateSubtitle) = when (modelState) {
                K2InferenceManager.State.READY -> Triple("Ready (Model Active)", Color(0xFF4CAF50), "Fast & Semantic AI active")
                K2InferenceManager.State.INFERENCE -> Triple("Analyzing with K2...", Color(0xFF00E5FF), "Evaluating multi-rule semantics")
                K2InferenceManager.State.LOADING -> Triple("Loading Model...", Color(0xFFFFA000), "Preparing on-device weights")
                K2InferenceManager.State.UNLOADING -> Triple("Freeing RAM...", Color(0xFFFFA000), "Reclaiming memory")
                K2InferenceManager.State.UNAVAILABLE -> Triple("Model Unavailable", Color.Red, "Import GGUF model to enable semantic AI")
                K2InferenceManager.State.ERROR -> Triple("Engine Error", Color.Red, "Check storage permission or reload")
                K2InferenceManager.State.UNINITIALIZED -> Triple("Standby (Zero RAM)", Color(0xFF81C784), "Wakes on semantic rules · 0% idle battery")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(stateColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(stateText, fontSize = 12.sp, color = stateColor, fontWeight = FontWeight.SemiBold)
                    Text(stateSubtitle, fontSize = 11.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun RulesSection(
    rules: List<NotificationRule>,
    onAddClick: () -> Unit,
    onToggle: (NotificationRule) -> Unit,
    onEdit: (NotificationRule) -> Unit,
    onDelete: (NotificationRule) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACTIVE RULES", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            TextButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("+ Add Rule")
            }
        }
        if (rules.isEmpty()) {
            Text("No rules defined. Tap '+ Add Rule' to configure custom routing.", fontSize = 13.sp, color = Color.Gray)
        } else {
            rules.forEach { rule ->
                val parsed = remember(rule.text) { RuleClassifier.classify(rule.text) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (rule.enabled) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = rule.enabled, onCheckedChange = { onToggle(rule) })
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onEdit(rule) }
                                .padding(horizontal = 4.dp)
                        ) {
                            Text(
                                rule.text,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (rule.enabled) MaterialTheme.colorScheme.onSurface else Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val isDeepRule = parsed.semanticDepth == "K2_DEEP" || rule.semanticDepth == "K2_DEEP"
                            // Rule engine badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isDeepRule) {
                                    Color(0xFF6A1B9A).copy(alpha = 0.18f)
                                } else when (parsed.intent) {
                                    RuleIntent.SIMPLE_CONTACT -> Color(0xFF1B5E20).copy(alpha = 0.15f)
                                    RuleIntent.SIMPLE_BLOCK -> Color(0xFFB71C1C).copy(alpha = 0.15f)
                                    RuleIntent.CONDITIONAL_CONTACT -> Color(0xFF0D47A1).copy(alpha = 0.15f)
                                    RuleIntent.TOPIC_FILTER -> Color(0xFF4A148C).copy(alpha = 0.15f)
                                    RuleIntent.APP_FILTER -> Color(0xFFE65100).copy(alpha = 0.15f)
                                }
                            ) {
                                Text(
                                    text = if (isDeepRule) {
                                        "🧠 K2 Deep AI (On-Demand)"
                                    } else when (parsed.intent) {
                                        RuleIntent.SIMPLE_CONTACT -> "⚡ Fast Contact (<0.2ms)"
                                        RuleIntent.SIMPLE_BLOCK -> "🚫 Direct Block (<0.2ms)"
                                        RuleIntent.CONDITIONAL_CONTACT -> "⚡ AOT Conditional (<0.2ms)"
                                        RuleIntent.TOPIC_FILTER -> "⚡ AOT Topic Filter (<0.2ms)"
                                        RuleIntent.APP_FILTER -> "⚡ App Filter (<0.2ms)"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeepRule) {
                                        Color(0xFFAB47BC)
                                    } else when (parsed.intent) {
                                        RuleIntent.SIMPLE_CONTACT -> Color(0xFF2E7D32)
                                        RuleIntent.SIMPLE_BLOCK -> Color(0xFFD32F2F)
                                        RuleIntent.CONDITIONAL_CONTACT -> Color(0xFF1976D2)
                                        RuleIntent.TOPIC_FILTER -> Color(0xFF7B1FA2)
                                        RuleIntent.APP_FILTER -> Color(0xFFEF6C00)
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        IconButton(onClick = { onEdit(rule) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Rule", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { onDelete(rule) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Rule", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RetentionSection(
    selectedPeriod: RetentionPeriod,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("RETENTION PERIOD", fontWeight = FontWeight.Bold)
                Text(
                    "Keep notifications for ${selectedPeriod.label}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            Text(
                "Change",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun NotificationItem(
    record: NotificationRecord,
    isImportant: Boolean,
    onClick: () -> Unit
) {
    val isK2Ai = record.reason.contains("K2 Deep AI") || record.reason.contains("🧠 K2") || record.reason.contains("K2 AI")
    val isFastRule = record.reason.contains("⚡ Fast") || record.reason.contains("[⚡ Fast") || record.reason.contains("⚡")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isImportant) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badge for App & Sender
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val appInfo = if (!record.sender.isNullOrBlank() && record.sender != record.appName) {
                        "${record.appName} · ${record.sender}"
                    } else {
                        record.appName
                    }
                    Text(
                        text = appInfo,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isImportant) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                // Engine tag + Urgency tag + timestamp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isK2Ai) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF7B1FA2).copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "🧠 K2 AI",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7B1FA2),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (isFastRule) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "⚡ Fast",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (record.important && record.alert) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "⚡ ALERT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    } else if (record.important) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 4.dp)
                        ) {
                            Text(
                                "⭐ IMPORTANT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(formatTime(record.timestamp), fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = record.summary,
                fontWeight = if (isImportant) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (record.reason.isNotBlank()) {
                Text(
                    text = record.reason,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun NotificationDetailDialog(
    record: NotificationRecord,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onAddRuleForSender: (String) -> Unit
) {
    val isK2Ai = record.reason.contains("🧠 K2 AI") || record.reason.contains("[🧠 K2 AI]")
    val isFastRule = record.reason.contains("⚡ Fast") || record.reason.contains("[⚡ Fast")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = record.summary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${record.appName}${if (!record.sender.isNullOrBlank()) " · " + record.sender else ""}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!record.title.isNullOrBlank() && record.title != record.summary) {
                    Text("Original Title:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text(record.title, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                if (!record.text.isNullOrBlank()) {
                    Text("Original Message:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                    Text(record.text, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                Text("AI Decision & Engine:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color.Gray)
                Text(record.reason, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category: ${record.aiCategory}", fontSize = 12.sp, color = Color.Gray)
                    Text(formatTime(record.timestamp), fontSize = 12.sp, color = Color.Gray)
                }

                if (record.important && record.alert) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("AI Chime Alert Triggered: YES (⚡)", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!record.sender.isNullOrBlank() && record.sender != record.appName) {
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onAddRuleForSender(record.sender)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Add Rule for '${record.sender}'")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddRuleDialog(
    initialText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    val suggestions = listOf(
        "Urgent messages from Mom are important",
        "Whatever message from Arjun related to movies is never important",
        "If someone messages about job related it is important",
        "OTP and bank transaction alerts are important",
        "Delivery and courier updates are important"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Personal Rule") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("e.g. Any message about job is important.") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Suggestions:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { text = suggestion },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditRuleDialog(
    rule: NotificationRule,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(rule.text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Rule") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RetentionDialog(
    currentPeriod: RetentionPeriod,
    onDismiss: () -> Unit,
    onSelect: (RetentionPeriod) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Notification Retention") },
        text = {
            Column {
                RetentionPeriod.entries.forEach { period ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(period) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = period == currentPeriod,
                            onClick = { onSelect(period) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(period.label, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

private fun formatTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> {
            val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
