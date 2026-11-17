package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.PermissionManagerHelper
import com.example.platform.AdvanceAlertBus
import com.example.platform.NexusSpeechAndTtsController
import com.example.platform.SpeechInputState
import com.example.ui.components.CreateReminderSheet
import com.example.ui.components.ReadinessSheet
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NexusTheme
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.containsDevanagari
import com.example.ui.viewmodel.DashboardViewModel

class MainActivity : ComponentActivity() {

    private lateinit var speechController: NexusSpeechAndTtsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        speechController = NexusSpeechAndTtsController(this).apply { initTts() }

        setContent {
            NexusTheme {
                val viewModel: DashboardViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val speechState by speechController.speechState.collectAsStateWithLifecycle()
                val lifecycleOwner = LocalLifecycleOwner.current

                var showComposerSheet by remember { mutableStateOf(false) }
                var showReadinessSheet by remember { mutableStateOf(false) }
                var pendingMicLaunch by remember { mutableStateOf(false) }
                var showMicRationale by remember { mutableStateOf(false) }
                var showNotificationRationale by remember { mutableStateOf(false) }
                var showExactAlarmRationale by remember { mutableStateOf(false) }

                // Refresh live permission state and date/time whenever returning to foreground
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshReadiness()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                        speechController.release()
                    }
                }

                // Gentle advance voice cue when an advance reminder fires while app is active
                LaunchedEffect(Unit) {
                    AdvanceAlertBus.events.collect { event ->
                        val cue = if (containsDevanagari(event.title)) {
                            "रिमाइंडर सूचना: ${event.advanceMinutes} मिनट में ${event.title}"
                        } else if (event.isAssignment) {
                            "Heads up: your assignment deadline for ${event.title} is in ${event.advanceMinutes} minutes."
                        } else {
                            "Heads up: ${event.title} is coming up in ${event.advanceMinutes} minutes."
                        }
                        speechController.speak(cue)
                        viewModel.showStatus("Advance reminder (${event.advanceMinutes}m): ${event.title}")
                    }
                }

                // Populate composer text when Android SpeechRecognizer produces partial/final transcript
                LaunchedEffect(speechState) {
                    when (val state = speechState) {
                        is SpeechInputState.Partial -> viewModel.updateComposerText(state.text)
                        is SpeechInputState.Result -> {
                            viewModel.updateComposerText(state.text)
                            speechController.resetSpeechState()
                        }
                        else -> Unit
                    }
                }

                val notificationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    viewModel.refreshReadiness()
                    if (granted) {
                        viewModel.showStatus("Notifications enabled for NEXUS local app alarms.")
                        if (!PermissionManagerHelper.canScheduleExactAlarms(this)) {
                            showExactAlarmRationale = true
                        }
                    } else {
                        viewModel.showStatus("Notifications declined. Reminders remain saved locally in-app.")
                    }
                }

                val micLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    viewModel.refreshReadiness()
                    if (granted) {
                        speechController.startListening(uiState.speechLanguageTag)
                    } else {
                        viewModel.showStatus("Microphone declined. Typed reminder input is always ready.")
                    }
                }

                fun launchAlertPreview(reminderId: Long) {
                    val intent = Intent(this, ReminderAlertActivity::class.java).apply {
                        putExtra(ReminderAlertActivity.EXTRA_REMINDER_ID, reminderId)
                    }
                    startActivity(intent)
                }

                fun handleContextualMicRequest() {
                    if (!PermissionManagerHelper.isSpeechRecognitionAvailable(this)) {
                        viewModel.showStatus("Speech recognition unavailable on this device. Please type your reminder.")
                        return
                    }
                    if (PermissionManagerHelper.hasMicrophonePermission(this)) {
                        speechController.startListening(uiState.speechLanguageTag)
                    } else {
                        showMicRationale = true
                    }
                }

                fun checkDeliveryPermissionsAfterSchedule(exactUsed: Boolean) {
                    val notifOk = PermissionManagerHelper.hasNotificationPermission(this)
                    if (!notifOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        showNotificationRationale = true
                    } else if (!exactUsed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        showExactAlarmRationale = true
                    }
                }

                DashboardScreen(
                    uiState = uiState,
                    onOpenComposer = {
                        speechController.resetSpeechState()
                        showComposerSheet = true
                    },
                    onOpenComposerWithMic = {
                        speechController.resetSpeechState()
                        showComposerSheet = true
                        pendingMicLaunch = true
                    },
                    onOpenReadiness = {
                        viewModel.refreshReadiness()
                        showReadinessSheet = true
                    },
                    onPreviewRingNow = {
                        viewModel.ensurePreviewReminder { id ->
                            launchAlertPreview(id)
                        }
                    },
                    onSchedule5sDemo = {
                        viewModel.createAndScheduleDelayedDemo(delaySeconds = 5) { _ ->
                            val notifOk = PermissionManagerHelper.hasNotificationPermission(this)
                            if (!notifOk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                showNotificationRationale = true
                            }
                        }
                    },
                    onSelectFilter = viewModel::selectFilter,
                    onCompleteReminder = viewModel::markComplete,
                    onCancelReminder = viewModel::cancelReminder,
                    onSnooze10m = { id -> viewModel.snoozeMinutes(id, 10) },
                    onRescheduleReminder = viewModel::rescheduleTo,
                    onDeleteReminder = viewModel::deleteReminder,
                    onRingReminderNow = { id -> launchAlertPreview(id) },
                    onLoadSampleSchedule = viewModel::seedSampleSchedule,
                    onDismissStatus = viewModel::clearStatusMessage
                )

                if (showComposerSheet) {
                    LaunchedEffect(pendingMicLaunch) {
                        if (pendingMicLaunch) {
                            pendingMicLaunch = false
                            handleContextualMicRequest()
                        }
                    }

                    CreateReminderSheet(
                        composerText = uiState.composerText,
                        parsedPreview = uiState.parsedPreview,
                        overrideScheduledAtMillis = uiState.overrideScheduledAtMillis,
                        overrideCategory = uiState.overrideCategory,
                        overrideAdvanceMinutes = uiState.overrideAdvanceMinutes,
                        speechLanguageTag = uiState.speechLanguageTag,
                        speechState = speechState,
                        readiness = uiState.readiness,
                        onTextChange = viewModel::updateComposerText,
                        onSelectTimeOverride = viewModel::setOverrideScheduledAt,
                        onSelectCategoryOverride = viewModel::setOverrideCategory,
                        onSelectAdvanceMinutes = viewModel::setOverrideAdvanceMinutes,
                        onSelectLanguage = viewModel::setSpeechLanguage,
                        onMicClick = { handleContextualMicRequest() },
                        onStopListening = { speechController.stopListening() },
                        onSubmit = {
                            speechController.stopListening()
                            viewModel.scheduleFromComposer { _, exactUsed ->
                                showComposerSheet = false
                                checkDeliveryPermissionsAfterSchedule(exactUsed)
                            }
                        },
                        onDismiss = {
                            speechController.stopListening()
                            showComposerSheet = false
                        }
                    )
                }

                if (showReadinessSheet) {
                    val readiness = uiState.readiness ?: PermissionManagerHelper.inspect(this)
                    ReadinessSheet(
                        readiness = readiness,
                        onRequestNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                startActivity(PermissionManagerHelper.buildNotificationSettingsIntent(this))
                            }
                        },
                        onOpenExactAlarmSettings = {
                            runCatching {
                                startActivity(PermissionManagerHelper.buildExactAlarmSettingsIntent(this))
                            }.onFailure {
                                startActivity(PermissionManagerHelper.buildAppSettingsIntent(this))
                            }
                        },
                        onOpenFullScreenIntentSettings = {
                            runCatching {
                                startActivity(PermissionManagerHelper.buildFullScreenIntentSettingsIntent(this))
                            }.onFailure {
                                startActivity(PermissionManagerHelper.buildAppSettingsIntent(this))
                            }
                        },
                        onRequestMicrophone = {
                            micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        onDismiss = { showReadinessSheet = false }
                    )
                }

                if (showMicRationale) {
                    AlertDialog(
                        onDismissRequest = { showMicRationale = false },
                        containerColor = MidnightNavy,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary,
                        title = { Text(stringResource(R.string.perm_mic_title)) },
                        text = { Text(stringResource(R.string.perm_mic_rationale)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showMicRationale = false
                                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = ObsidianNavy
                                )
                            ) {
                                Text("Allow Microphone")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMicRationale = false }) {
                                Text("Type Instead", color = TextSecondary)
                            }
                        }
                    )
                }

                if (showNotificationRationale) {
                    AlertDialog(
                        onDismissRequest = { showNotificationRationale = false },
                        containerColor = MidnightNavy,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary,
                        title = { Text(stringResource(R.string.perm_notif_title)) },
                        text = { Text(stringResource(R.string.perm_notif_rationale)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showNotificationRationale = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = ObsidianNavy
                                )
                            ) {
                                Text("Enable Notifications")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showNotificationRationale = false }) {
                                Text("Not Now", color = TextSecondary)
                            }
                        }
                    )
                }

                if (showExactAlarmRationale) {
                    AlertDialog(
                        onDismissRequest = { showExactAlarmRationale = false },
                        containerColor = MidnightNavy,
                        titleContentColor = TextPrimary,
                        textContentColor = TextSecondary,
                        title = { Text(stringResource(R.string.perm_exact_title)) },
                        text = { Text(stringResource(R.string.perm_exact_rationale)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showExactAlarmRationale = false
                                    runCatching {
                                        startActivity(PermissionManagerHelper.buildExactAlarmSettingsIntent(this))
                                    }.onFailure {
                                        startActivity(PermissionManagerHelper.buildAppSettingsIntent(this))
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = ObsidianNavy
                                )
                            ) {
                                Text("Open Exact Alarm Settings")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExactAlarmRationale = false }) {
                                Text("Keep Fallback", color = TextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }
}
