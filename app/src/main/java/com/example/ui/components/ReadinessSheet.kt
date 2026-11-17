package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.CapabilityReadiness
import com.example.ui.theme.AmberContainer
import com.example.ui.theme.AmberSignal
import com.example.ui.theme.BorderNavy
import com.example.ui.theme.ElectricTeal
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.MidnightNavy
import com.example.ui.theme.ObsidianNavy
import com.example.ui.theme.SurfaceNavy
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessSheet(
    readiness: CapabilityReadiness,
    onRequestNotifications: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
    onOpenFullScreenIntentSettings: () -> Unit,
    onRequestMicrophone: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MidnightNavy,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Alarm & Permission Readiness",
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary
            )
            Text(
                text = "NEXUS never requests permissions on launch. Grant only what you need—every capability degrades gracefully if denied.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            PermissionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = stringResource(R.string.perm_notif_title),
                rationale = stringResource(R.string.perm_notif_rationale),
                granted = readiness.notificationsGranted,
                actionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "Enable Notifications" else "Enabled",
                buttonTag = "grant_notifications_button",
                onAction = onRequestNotifications
            )

            PermissionCard(
                icon = Icons.Rounded.Alarm,
                title = stringResource(R.string.perm_exact_title),
                rationale = stringResource(R.string.perm_exact_rationale),
                granted = readiness.exactAlarmsGranted,
                actionLabel = "Open Exact Alarm Settings",
                buttonTag = "grant_exact_alarm_button",
                onAction = onOpenExactAlarmSettings
            )

            PermissionCard(
                icon = Icons.Rounded.Fullscreen,
                title = stringResource(R.string.perm_fsi_title),
                rationale = stringResource(R.string.perm_fsi_rationale),
                granted = readiness.fullScreenIntentGranted,
                actionLabel = "Allow Full-Screen Alerts",
                buttonTag = "grant_fsi_button",
                onAction = onOpenFullScreenIntentSettings
            )

            PermissionCard(
                icon = Icons.Rounded.Mic,
                title = stringResource(R.string.perm_mic_title),
                rationale = stringResource(R.string.perm_mic_rationale) +
                    if (!readiness.speechRecognitionAvailable) "\n(Note: SpeechRecognizer service not detected on this device—typed input works normally.)" else "",
                granted = readiness.microphoneGranted,
                actionLabel = "Enable Microphone",
                buttonTag = "grant_mic_button",
                onAction = onRequestMicrophone
            )

            // OEM & Android Version Restrictions Guide
            Surface(
                color = SurfaceNavy,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, BorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhoneAndroid,
                            contentDescription = null,
                            tint = ElectricTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Android Version & OEM Restrictions",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "• Android 12+ (API 31+): SCHEDULE_EXACT_ALARM requires user approval in Alarms & Reminders; if denied, NEXUS falls back to setAndAllowWhileIdle().\n" +
                            "• Android 13+ (API 33+): POST_NOTIFICATIONS required for lock-screen/heads-up delivery.\n" +
                            "• Android 14+ (API 34+): USE_FULL_SCREEN_INTENT is user-controlled for non-calling apps; without it, NEXUS posts a high-priority heads-up notification.\n" +
                            "• Xiaomi / MIUI & HyperOS: Enable Autostart & 'Show on Lock Screen' in App Info.\n" +
                            "• Samsung One UI: Exclude NEXUS AI from 'Deep Sleeping Apps' under Battery.\n" +
                            "• OnePlus / Oppo / Vivo: Allow Background Activity so AlarmManager broadcasts wake reliably.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    rationale: String,
    granted: Boolean,
    actionLabel: String,
    buttonTag: String,
    onAction: () -> Unit
) {
    Surface(
        color = SurfaceNavy,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (granted) EmeraldSuccess.copy(alpha = 0.4f) else AmberSignal.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (granted) EmeraldSuccess else AmberSignal,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = if (granted) EmeraldContainer else AmberContainer,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (granted) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = if (granted) EmeraldSuccess else AmberSignal,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (granted) "Ready" else "Fallback",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (granted) EmeraldSuccess else AmberSignal
                        )
                    }
                }
            }

            Text(
                text = rationale,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (!granted) {
                Spacer(modifier = Modifier.size(2.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(buttonTag),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricTeal,
                        contentColor = ObsidianNavy
                    )
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
