# NEXUS AI — Personal Reminder & Voice Follow-Up Prototype

**NEXUS AI** is a 100% offline-first native Android personal reminder and follow-up prototype built with Kotlin, Jetpack Compose (Material 3), Room, `AlarmManager`, Android `TextToSpeech`, and Android `SpeechRecognizer`.

> **Local App Alarm Notice:** The incoming-call-style full-screen alert is a **local app alarm experience** (`NotificationCompat.CATEGORY_ALARM`), **not** a cellular/PSTN call. There is no outbound calling, no infinite repeat calling loop, no backend/cloud deployment, no API keys, and no data upload.

---

## Key Features

- **Today Overview & Midnight Rollover (`DailyScheduleCalculator`):**
  - Displays today's formatted date in the device's local timezone and a progress bar with `completed / total` count and percentage for tasks scheduled for today.
  - Preserves completed and cancelled task history in Room while recalculating today's daily count for the local calendar day (automatically rolling over at local midnight).
- **Today Chronological Timeline:**
  - Lists every task/reminder for today in chronological order, numbered **Task 1**, **Task 2**, etc., showing title, scheduled/due time, category, advance heads-up interval, and live status (`Upcoming`, `Due Now`, `Overdue`, `Completed`, `Cancelled`).
  - Supports inline **Complete** and **Cancel** actions right from the timeline alongside existing `Upcoming`, `Assignments`, `Completed`, and `All` filters and the **Pending Assignments** banner.
- **Advance Reminders & Assignment Deadlines:**
  - **Ordinary Tasks:** User can choose **15 min** or **20 min (default)** before due time for a gentle advance notification (plus spoken voice cue when foregrounded), followed by the ringing local app alarm at due time if still incomplete.
  - **Assignment Deadlines:** Includes an explicit Assignment Deadline field and offers **30 min** or **45 min (default)** advance reminders before the deadline.
  - **Smart Future-Only Guard:** If the due time or deadline is less than the selected advance interval away, NEXUS schedules only reminders still in the future without creating duplicate alarms.
- **Completion & Cancellation Correctness:**
  - Completing or cancelling a task immediately cancels **both** its advance and due alarms (`AlarmManager`) and clears both notifications.
  - Completed or cancelled tasks never ring.
  - Incomplete items at due time transition to `Overdue` / active and ring once via the local app alarm screen with **Mark Complete**, **Snooze 10m**, **Snooze 1h**, **Tomorrow 6 AM**, **Cancel Task**, and **Talk to NEXUS** actions.
- **Natural Language Input & Bilingual Support (English & हिन्दी):**
  - On-device parser (`NaturalLanguageReminderParser`) supports English, Hinglish, and Hindi (Devanagari) reminders plus voice follow-up commands.
- **Incoming-Call-Style Local App Alarm (`ReminderAlertActivity`):**
  - Pulsing sonar ring animation, reminder text, **Dismiss** and **Talk to NEXUS** actions, and Android `TextToSpeech` spoken readout (automatically selecting `en-IN` or `hi-IN`).
- **Contextual Permission Onboarding & Graceful Degradation:**
  - Zero permissions requested on launch. Rationales and fallbacks are provided for `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `USE_FULL_SCREEN_INTENT`, and `RECORD_AUDIO`.

---

## Project Setup & Offline Build

1. Open the project in Android Studio (Meerkat / Narwhal or newer with JDK 17+ / AGP 9.x).
2. **No secrets or API keys required:** `.env.example` contains only commented-out placeholders; cloud/network dependencies remain disabled in `app/build.gradle.kts`.
3. Build and run unit + Robolectric tests:
   ```bash
   gradle :app:assembleDebug
   gradle :app:testDebugUnitTest
   ```

---

## Android Version & OEM Vendor Restrictions

| Capability / Restriction | Android Behavior | NEXUS AI Handling & Graceful Degradation |
| :--- | :--- | :--- |
| **Notifications (`POST_NOTIFICATIONS`)** | Required at runtime on **Android 13+ (API 33+)**. | Explained contextually after scheduling or in **Readiness**. If denied, reminders still save locally in Room and trigger in-app when active. |
| **Exact Alarms (`SCHEDULE_EXACT_ALARM`)** | Restricted by default on **Android 12+ (API 31+)** unless granted by user in *Alarms & Reminders* settings. | Uses `setAlarmClock` / `setExactAndAllowWhileIdle` when permitted; gracefully falls back to `setAndAllowWhileIdle()` when denied. |
| **Full-Screen Intent (`USE_FULL_SCREEN_INTENT`)** | Special user-controlled permission on **Android 14+ (API 34+)**. | Attaches `setFullScreenIntent()` when allowed; otherwise posts a high-priority heads-up notification with *Talk to NEXUS*, *Snooze 10m*, and *Mark Complete* actions. |
| **Microphone (`RECORD_AUDIO`)** | Dangerous runtime permission. | Requested only when tapping the mic button. Gracefully degrades to typed input and touch follow-up buttons if denied or unavailable. |
| **Xiaomi / Samsung / OnePlus / Oppo / Vivo** | OEM battery savers & autostart policies may delay background alarms. | Detailed per-OEM tips are built into the in-app **Readiness** sheet (*Autostart*, *Deep Sleeping Apps*, *Background Activity*). |

---

## Manual Verification Checklist

1. **Zero Launch Prompts:** Freshly launch NEXUS AI and confirm **no** system permission dialogs appear automatically.
2. **Today Overview & Local Date:** Confirm Today Overview displays today's date in the device's local timezone and starts at `0/0 completed (0%)`.
3. **Chronological Today Timeline:** Create two tasks for today (e.g., `6:00 AM` and `7:00 AM`). Confirm they appear in chronological order numbered **Task 1** and **Task 2** with time, category, and `Upcoming` status.
4. **Task & Assignment Advance Intervals:**
   - Open composer for an ordinary task -> verify **15 min** / **20 min (Default)** advance chips appear.
   - Switch category to **Assignment** -> verify **Assignment Deadline** label and **30 min** / **45 min (Default)** advance chips appear.
   - Pick a due time 10 minutes from now -> verify the UI notes that only the due-time alarm will be scheduled (no past/duplicate advance alarm).
5. **Completion & Cancellation Alarm Cancellation:** Mark **Task 1** complete (or cancel it) from the Today timeline -> verify Today Overview progress updates immediately, history stays visible under `Completed` / `All`, and its pending advance/due alarms never ring.
6. **Local App Alarm & Talk to NEXUS:** Tap **Preview Local Alarm** or **Test 5s Local Alarm** -> verify **NEXUS AI • LOCAL APP ALARM** (*Local app alarm experience • Not a cellular call*) appears with ringing pulse -> tap **Talk to NEXUS** -> verify ringing stops and Android `TextToSpeech` speaks the reminder aloud -> verify **Mark Complete**, **Snooze 10m**, **Snooze 1h**, **Tomorrow 6 AM**, and **Cancel Task** controls work cleanly without infinite repeat ringing.
7. **Midnight Rollover:** Advance the device date past midnight -> verify completed tasks remain in history while Today Overview recalculates for the new local calendar day.
