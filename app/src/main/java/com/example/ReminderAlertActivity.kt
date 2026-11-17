package com.example

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.ReminderCategory
import com.example.data.ReminderEntity
import com.example.data.ReminderStatus
import com.example.domain.FollowUpCommand
import com.example.domain.NaturalLanguageReminderParser
import com.example.domain.PermissionManagerHelper
import com.example.platform.AlarmRingtonePlayer
import com.example.platform.NexusSpeechAndTtsController
import com.example.platform.ReminderNotificationHelper
import com.example.platform.SpeechInputState
import com.example.ui.screens.IncomingReminderScreen
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.NexusTheme
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.containsDevanagari
import kotlinx.coroutines.launch

class ReminderAlertActivity : ComponentActivity() {

    private lateinit var ringtonePlayer: AlarmRingtonePlayer
    private lateinit var speechController: NexusSpeechAndTtsController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenBehavior()

        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val autoAnswer = intent.getBooleanExtra(EXTRA_AUTO_ANSWER, false)
        val app = application as NexusApplication
        val repository = app.repository

        ringtonePlayer = AlarmRingtonePlayer(this)
        speechController = NexusSpeechAndTtsController(this).apply { initTts() }

        setContent {
            NexusTheme {
                var reminderState by remember {
                    mutableStateOf(
                        ReminderEntity(
                            id = reminderId,
                            title = "Finish my mathematics assignment",
                            originalInput = "Remind me at 6 AM to finish my mathematics assignment",
                            category = ReminderCategory.ASSIGNMENT,
                            scheduledAtMillis = System.currentTimeMillis(),
                            status = ReminderStatus.RINGING,
                            isAssignment = true
                        )
                    )
                }
                var isAnswered by remember { mutableStateOf(autoAnswer) }
                var followUpTranscript by remember { mutableStateOf("") }
                var showMicRationale by remember { mutableStateOf(false) }

                val isSpeaking by speechController.isSpeaking.collectAsStateWithLifecycle()
                val speechState by speechController.speechState.collectAsStateWithLifecycle()

                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        val lang = if (containsDevanagari(reminderState.title)) "hi-IN" else "en-IN"
                        speechController.startListening(lang)
                    } else {
                        followUpTranscript = "Microphone declined. Use the touch follow-up buttons below anytime."
                    }
                }

                fun buildSpokenMessage(entity: ReminderEntity): String {
                    return if (containsDevanagari(entity.title)) {
                        "नमस्ते, यह आपका नेक्सस लोकल ऐप अलार्म है: ${entity.title}। क्या आपने इसे पूरा कर लिया है, या आप स्नूज़ करना चाहेंगे?"
                    } else {
                        val kind = if (entity.isAssignment) "assignment deadline" else "reminder"
                        "Hello, this is your NEXUS local app alarm for your $kind: ${entity.title}. Have you completed it, or would you like to snooze it?"
                    }
                }

                LaunchedEffect(reminderId) {
                    if (reminderId > 0L) {
                        val loaded = repository.getReminderById(reminderId)
                        if (loaded == null || !loaded.isActiveForAlarm) {
                            // Completed or cancelled tasks must never ring
                            ringtonePlayer.stopRinging()
                            ReminderNotificationHelper.cancelNotification(this@ReminderAlertActivity, reminderId)
                            app.scheduler.cancel(reminderId)
                            finish()
                            return@LaunchedEffect
                        }
                        reminderState = loaded
                        followUpTranscript = buildSpokenMessage(loaded)
                    } else {
                        followUpTranscript = buildSpokenMessage(reminderState)
                    }

                    if (autoAnswer) {
                        ringtonePlayer.stopRinging()
                        speechController.speak(followUpTranscript)
                    } else {
                        ringtonePlayer.startRinging()
                    }
                }

                LaunchedEffect(speechState) {
                    val result = speechState as? SpeechInputState.Result ?: return@LaunchedEffect
                    val command = NaturalLanguageReminderParser.parseFollowUpIntent(result.text)
                    when (command) {
                        FollowUpCommand.Complete -> {
                            speechController.speak("Marked complete.")
                            if (reminderId > 0L) repository.markComplete(reminderId)
                            ReminderNotificationHelper.cancelNotification(this@ReminderAlertActivity, reminderId)
                            finish()
                        }

                        is FollowUpCommand.SnoozeMinutes -> {
                            speechController.speak("Snoozed for ${command.minutes} minutes.")
                            if (reminderId > 0L) repository.snoozeByMinutes(reminderId, command.minutes)
                            ReminderNotificationHelper.cancelNotification(this@ReminderAlertActivity, reminderId)
                            finish()
                        }

                        FollowUpCommand.TomorrowMorning -> {
                            speechController.speak("Rescheduled for tomorrow at 6 AM.")
                            if (reminderId > 0L) repository.rescheduleTomorrowMorning(reminderId, 6)
                            ReminderNotificationHelper.cancelNotification(this@ReminderAlertActivity, reminderId)
                            finish()
                        }

                        FollowUpCommand.Repeat -> {
                            val msg = buildSpokenMessage(reminderState)
                            followUpTranscript = msg
                            speechController.speak(msg)
                        }

                        FollowUpCommand.Dismiss -> {
                            if (reminderId > 0L) repository.markOverdueIfActive(reminderId)
                            ReminderNotificationHelper.cancelNotification(this@ReminderAlertActivity, reminderId)
                            finish()
                        }

                        is FollowUpCommand.Unknown -> {
                            followUpTranscript = "Heard \"${command.transcript}\". Tap Complete, Snooze, Cancel, or say \"Done\" / \"Snooze 10 minutes\"."
                            speechController.resetSpeechState()
                        }
                    }
                }

                DisposableEffect(Unit) {
                    onDispose {
                        ringtonePlayer.stopRinging()
                        speechController.release()
                    }
                }

                BackHandler {
                    ringtonePlayer.stopRinging()
                    speechController.stopSpeaking()
                    lifecycleScope.launch {
                        if (reminderId > 0L) {
                            repository.markOverdueIfActive(reminderId)
                        }
                        finish()
                    }
                }

                IncomingReminderScreen(
                    reminder = reminderState,
                    isAnswered = isAnswered,
                    isSpeaking = isSpeaking,
                    speechState = speechState,
                    followUpTranscript = followUpTranscript.ifBlank { buildSpokenMessage(reminderState) },
                    onDismissAlarm = {
                        ringtonePlayer.stopRinging()
                        speechController.stopSpeaking()
                        ReminderNotificationHelper.cancelNotification(this, reminderId)
                        lifecycleScope.launch {
                            if (reminderId > 0L) {
                                // Remain active/overdue if incomplete, without creating infinite repeat rings
                                repository.markOverdueIfActive(reminderId)
                            }
                            finish()
                        }
                    },
                    onTalkToNexus = {
                        ringtonePlayer.stopRinging()
                        isAnswered = true
                        val spoken = buildSpokenMessage(reminderState)
                        followUpTranscript = spoken
                        speechController.speak(spoken)
                    },
                    onRepeatVoice = {
                        val spoken = buildSpokenMessage(reminderState)
                        followUpTranscript = spoken
                        speechController.speak(spoken)
                    },
                    onVoiceReply = {
                        speechController.stopSpeaking()
                        if (PermissionManagerHelper.hasMicrophonePermission(this)) {
                            val lang = if (containsDevanagari(reminderState.title)) "hi-IN" else "en-IN"
                            speechController.startListening(lang)
                        } else {
                            showMicRationale = true
                        }
                    },
                    onMarkComplete = {
                        ringtonePlayer.stopRinging()
                        speechController.stopSpeaking()
                        ReminderNotificationHelper.cancelNotification(this, reminderId)
                        lifecycleScope.launch {
                            if (reminderId > 0L) {
                                repository.markComplete(reminderId)
                            }
                            finish()
                        }
                    },
                    onCancelTask = {
                        ringtonePlayer.stopRinging()
                        speechController.stopSpeaking()
                        ReminderNotificationHelper.cancelNotification(this, reminderId)
                        lifecycleScope.launch {
                            if (reminderId > 0L) {
                                repository.cancelReminder(reminderId)
                            }
                            finish()
                        }
                    },
                    onSnoozeMinutes = { mins ->
                        ringtonePlayer.stopRinging()
                        speechController.stopSpeaking()
                        ReminderNotificationHelper.cancelNotification(this, reminderId)
                        lifecycleScope.launch {
                            if (reminderId > 0L) {
                                repository.snoozeByMinutes(reminderId, mins)
                            }
                            finish()
                        }
                    },
                    onTomorrowMorning = {
                        ringtonePlayer.stopRinging()
                        speechController.stopSpeaking()
                        ReminderNotificationHelper.cancelNotification(this, reminderId)
                        lifecycleScope.launch {
                            if (reminderId > 0L) {
                                repository.rescheduleTomorrowMorning(reminderId, 6)
                            }
                            finish()
                        }
                    },
                    onCloseScreen = {
                        ringtonePlayer.stopRinging()
                        speechController.stopSpeaking()
                        ReminderNotificationHelper.cancelNotification(this, reminderId)
                        lifecycleScope.launch {
                            if (reminderId > 0L) {
                                repository.markOverdueIfActive(reminderId)
                            }
                            finish()
                        }
                    }
                )

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
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = ObsidianNavy
                                )
                            ) {
                                Text("Continue")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showMicRationale = false }) {
                                Text("Use Touch Controls", color = TextSecondary)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun configureLockScreenBehavior() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_AUTO_ANSWER = "extra_auto_answer"
    }
}
