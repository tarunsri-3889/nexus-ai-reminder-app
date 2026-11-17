package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ReminderEntity
import com.example.domain.NaturalLanguageReminderParser
import com.example.platform.SpeechInputState
import com.example.ui.components.AdaptiveText
import com.example.ui.components.CategoryBadge
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CoralContainer
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SkyCyan
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TealContainer
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IncomingReminderScreen(
    reminder: ReminderEntity,
    isAnswered: Boolean,
    isSpeaking: Boolean,
    speechState: SpeechInputState,
    followUpTranscript: String,
    onDismissAlarm: () -> Unit,
    onTalkToNexus: () -> Unit,
    onRepeatVoice: () -> Unit,
    onVoiceReply: () -> Unit,
    onMarkComplete: () -> Unit,
    onCancelTask: () -> Unit,
    onSnoozeMinutes: (Int) -> Unit,
    onTomorrowMorning: () -> Unit,
    onCloseScreen: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ObsidianNavy, MidnightNavy, ObsidianNavy)
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Identity & Explicit Non-Cellular-Call Disclaimer
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = if (isAnswered) TealContainer else SurfaceNavy,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ElectricTeal.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isAnswered) Icons.Rounded.GraphicEq else Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = stringResource(R.string.incoming_alarm_header),
                            style = MaterialTheme.typography.labelLarge,
                            color = ElectricTeal
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.incoming_alarm_disclaimer),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Center Orb + Ringing / Speaking Animation + Reminder Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(210.dp)
                ) {
                    if (!isAnswered || isSpeaking) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val ringColor = if (isAnswered) SkyCyan else ElectricTeal
                            drawCircle(
                                color = ringColor.copy(alpha = pulseAlpha),
                                radius = (size.minDimension / 2f) * pulseScale.coerceAtMost(1.0f),
                                style = Stroke(width = 4.dp.toPx())
                            )
                            drawCircle(
                                color = ringColor.copy(alpha = (pulseAlpha * 0.6f)),
                                radius = (size.minDimension / 2f) * (0.72f + 0.24f * (pulseScale - 0.92f)),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        border = BorderStroke(
                            2.dp,
                            if (isAnswered) SkyCyan else ElectricTeal
                        ),
                        modifier = Modifier
                            .size(144.dp)
                            .scale(if (!isAnswered) 0.98f + (pulseScale - 0.92f) * 0.14f else 1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_nexus_orb_1794904916886),
                            contentDescription = "NEXUS AI Voice Core",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                }

                Text(
                    text = when {
                        isAnswered && isSpeaking -> "NEXUS is speaking aloud…"
                        isAnswered -> stringResource(R.string.incoming_alarm_active)
                        else -> stringResource(R.string.incoming_alarm_ringing)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isAnswered) SkyCyan else ElectricTeal
                )

                CategoryBadge(
                    category = reminder.category,
                    isAssignment = reminder.isAssignment
                )

                AdaptiveText(
                    text = reminder.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    preferDisplay = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Scheduled: ${NaturalLanguageReminderParser.formatDateTimeShort(reminder.scheduledAtMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Action Area (Ringing Controls vs Talk-to-NEXUS Follow-up Controls)
            AnimatedContent(
                targetState = isAnswered,
                label = "alarm_mode_transition",
                modifier = Modifier.fillMaxWidth()
            ) { answered ->
                if (!answered) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Button(
                                onClick = onDismissAlarm,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(62.dp)
                                    .testTag("alarm_dismiss_button"),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CoralContainer,
                                    contentColor = CoralAlert
                                ),
                                border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_dismiss),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = onTalkToNexus,
                                modifier = Modifier
                                    .weight(1.25f)
                                    .height(62.dp)
                                    .testTag("alarm_talk_button"),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = ObsidianNavy
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PhoneInTalk,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.action_talk_to_nexus),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { onSnoozeMinutes(10) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("alarm_snooze_10m_button"),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, BorderNavy)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Snooze,
                                contentDescription = null,
                                tint = AmberSignal,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.action_snooze_10m),
                                color = AmberSignal
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            color = SurfaceNavy,
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, SkyCyan.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "NEXUS Follow-Up",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = SkyCyan
                                    )
                                    OutlinedButton(
                                        onClick = onRepeatVoice,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, BorderNavy)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                            contentDescription = "Repeat spoken reminder",
                                            tint = ElectricTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.action_repeat_voice),
                                            color = ElectricTeal,
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }

                                AdaptiveText(
                                    text = followUpTranscript,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )

                                if (speechState !is SpeechInputState.Idle) {
                                    val speechMsg = when (speechState) {
                                        is SpeechInputState.Listening -> "Listening… say \"Mark complete\", \"Snooze 10 minutes\", or \"Tomorrow\"."
                                        is SpeechInputState.Partial -> "Hearing: \"${speechState.text}\""
                                        is SpeechInputState.Result -> "Heard: \"${speechState.text}\""
                                        is SpeechInputState.Error -> speechState.message
                                        SpeechInputState.Idle -> ""
                                    }
                                    Text(
                                        text = speechMsg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (speechState is SpeechInputState.Error) CoralAlert else ElectricTeal
                                    )
                                }
                            }
                        }

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = onMarkComplete,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("followup_complete_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldContainer,
                                    contentColor = EmeraldSuccess
                                ),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.action_mark_complete), fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onSnoozeMinutes(10) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("followup_snooze_10m_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AmberSignal.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Snooze,
                                    contentDescription = null,
                                    tint = AmberSignal,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Snooze 10m", color = AmberSignal)
                            }

                            OutlinedButton(
                                onClick = { onSnoozeMinutes(60) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("followup_snooze_1h_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderNavy)
                            ) {
                                Text("Snooze 1h", color = SkyCyan)
                            }

                            OutlinedButton(
                                onClick = onTomorrowMorning,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("followup_tomorrow_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderNavy)
                            ) {
                                Text("Tomorrow 6 AM", color = TextPrimary)
                            }

                            OutlinedButton(
                                onClick = onCancelTask,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("followup_cancel_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, CoralAlert.copy(alpha = 0.5f))
                            ) {
                                Text("Cancel Task", color = CoralAlert)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onVoiceReply,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("followup_voice_reply_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ElectricTeal,
                                    contentColor = ObsidianNavy
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Speak Reply", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = onCloseScreen,
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("followup_close_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BorderNavy)
                            ) {
                                Text("Close", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
