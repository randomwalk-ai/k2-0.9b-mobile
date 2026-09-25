package com.example.llama.aichat.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.llama.aichat.ai.K2InferenceManager
import com.example.llama.aichat.data.AppDatabase
import com.example.llama.aichat.data.NotificationRepository
import com.example.llama.aichat.data.NotificationRuleRepository

class NotificationListener : NotificationListenerService() {
    private lateinit var processor: NotificationProcessor
    private lateinit var inferenceManager: K2InferenceManager

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.getDatabase(this)
        val notificationRepo = NotificationRepository(db.notificationDao())
        val ruleRepo = NotificationRuleRepository(db.notificationRuleDao())

        inferenceManager = K2InferenceManager.getInstance(this)
        val summaryManager = NotificationSummaryManager(this)
        val alertManager = AIAlertManager(this)

        processor = NotificationProcessor(
            this,
            inferenceManager,
            notificationRepo,
            ruleRepo,
            summaryManager,
            alertManager
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        // 1. Skip our own app notifications to avoid loops
        if (sbn.packageName == packageName) return

        val notification = sbn.notification ?: return

        // 2. Skip grouped summary containers to prevent duplicate concatenated entries
        val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 || NotificationCompat.isGroupSummary(notification)
        if (isGroupSummary) {
            Log.d("NotificationListener", "Skipping group summary container from ${sbn.packageName}")
            return
        }

        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        var sender: String? = null

        // 3. Extract message text for MessagingStyle notifications (WhatsApp, Telegram, Signal)
        try {
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            if (!messages.isNullOrEmpty()) {
                val lastMsg = messages.lastOrNull()
                if (lastMsg is android.os.Bundle) {
                    val msgText = lastMsg.getCharSequence("text")?.toString()?.trim()
                    if (!msgText.isNullOrBlank()) {
                        text = msgText
                    }
                    val msgSender = lastMsg.getCharSequence("sender")?.toString()?.trim()
                    if (!msgSender.isNullOrBlank() && !msgSender.equals("You", ignoreCase = true)) {
                        sender = msgSender
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("NotificationListener", "Error extracting message text: ${e.message}")
        }

        if (text.isNullOrBlank() && !bigText.isNullOrBlank()) {
            text = bigText
        }

        // 4. Extract latest line for InboxStyle notifications
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (!lines.isNullOrEmpty() && (text.isNullOrBlank() || text!!.contains("new message", ignoreCase = true))) {
            val lastLine = lines.lastOrNull()?.toString()?.trim()
            if (!lastLine.isNullOrBlank()) {
                text = lastLine
            }
        }

        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()

        val textAndTitleLower = "${title ?: ""} ${text ?: ""} ${bigText ?: ""} ${subText ?: ""}".lowercase()
        val packageLower = sbn.packageName.lowercase()

        // =========================================================================
        // OUTBOUND & USER ACTION FILTER (Zero Storage / Zero Noise)
        // Completely drop progress bars, user uploads, ongoing status, and screenshots
        // =========================================================================

        // A. Uploads, Downloads, Progress Bars (e.g. "Story uploading... 83%", "Story uploaded!")
        val hasProgress = extras.containsKey(Notification.EXTRA_PROGRESS) || extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
        val isProgressOrUpload = hasProgress ||
                textAndTitleLower.contains("uploading") ||
                textAndTitleLower.contains("story uploaded") ||
                textAndTitleLower.contains("post uploaded") ||
                textAndTitleLower.contains("downloading") ||
                textAndTitleLower.contains("saving...") ||
                textAndTitleLower.contains("sending...")

        if (isProgressOrUpload) {
            Log.d("NotificationListener", "Dropped outbound progress/upload notification from ${sbn.packageName}")
            return
        }

        // B. Screenshots & Screen Captures
        val isScreenshot = packageLower.contains("smartcapture") || packageLower.contains("screencapture") ||
                packageLower.contains("screenshot") || textAndTitleLower.contains("screenshot")
        if (isScreenshot) {
            Log.d("NotificationListener", "Dropped screenshot notification from ${sbn.packageName}")
            return
        }

        // C. Ongoing Media Controls & Active GPS Navigation
        val isMediaOrNav = notification.category == Notification.CATEGORY_TRANSPORT ||
                notification.category == Notification.CATEGORY_NAVIGATION ||
                packageLower.contains("spotify") ||
                packageLower.contains("music") ||
                packageLower.contains("audio") ||
                packageLower.contains("maps") ||
                packageLower.contains("waze")

        if (isMediaOrNav && sbn.isOngoing) {
            Log.d("NotificationListener", "Dropped ongoing media/nav session from ${sbn.packageName}")
            return
        }

        // D. Outgoing Calls & Active In-Call Progress
        val actions = notification.actions
        val hasAnswerAction = actions?.any { action ->
            val actionTitle = action.title?.toString() ?: ""
            actionTitle.contains("Answer", ignoreCase = true) ||
            actionTitle.contains("Accept", ignoreCase = true) ||
            actionTitle.contains("Incoming", ignoreCase = true)
        } ?: false

        val isCallCategory = notification.category == Notification.CATEGORY_CALL ||
                packageLower.contains("dialer") ||
                packageLower.contains("telecom") ||
                packageLower.contains("phone")

        val isMissedCall = notification.category == Notification.CATEGORY_MISSED_CALL ||
                (title?.contains("missed call", ignoreCase = true) == true) ||
                (text?.contains("missed call", ignoreCase = true) == true)

        val isIncomingCall = hasAnswerAction || isMissedCall

        if (isCallCategory && sbn.isOngoing && !isIncomingCall) {
            Log.d("NotificationListener", "Dropped outgoing/in-call session from ${sbn.packageName}")
            return
        }

        // E. System Hardware Status & Device Maintenance Scans
        val isSystemStatus = (packageLower == "com.android.systemui" || packageLower == "android" || packageLower.contains("devicesecurity")) &&
                (textAndTitleLower.contains("charging") || textAndTitleLower.contains("battery") ||
                 textAndTitleLower.contains("usb") || textAndTitleLower.contains("scanning phone"))
        if (isSystemStatus) {
            Log.d("NotificationListener", "Dropped system hardware/maintenance indicator from ${sbn.packageName}")
            return
        }

        // F. Sync Check Placeholders
        val isSyncPlaceholder = textAndTitleLower.contains("checking for new messages") || textAndTitleLower.contains("searching for new messages")
        if (isSyncPlaceholder) {
            Log.d("NotificationListener", "Dropped sync placeholder from ${sbn.packageName}")
            return
        }

        // Fallback sender extraction for inbound messages
        if ((sender.isNullOrBlank() || sender.equals("You", ignoreCase = true)) && !isMediaOrNav) {
            val convTitle = extras.getCharSequence(NotificationCompat.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()
            if (!convTitle.isNullOrBlank() && !convTitle.equals("You", ignoreCase = true)) {
                sender = convTitle
            }
        }

        if ((sender.isNullOrBlank() || sender.equals("You", ignoreCase = true)) && !isMediaOrNav) {
            if (!title.isNullOrBlank() && !title.equals("You", ignoreCase = true)) {
                sender = title
            }
        }

        if (title.isNullOrBlank() && text.isNullOrBlank()) {
            Log.d("NotificationListener", "Skipping empty notification from ${sbn.packageName}")
            return
        }

        val appName = try {
            val pm = packageManager
            val ai = pm.getApplicationInfo(sbn.packageName, 0)
            pm.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val notificationData = NotificationData(
            packageName = sbn.packageName,
            appName = appName,
            title = title,
            text = text,
            subText = subText,
            sender = sender,
            category = notification.category,
            notificationKey = sbn.key ?: "${sbn.packageName}_${sbn.id}_${sbn.postTime}",
            timestamp = sbn.postTime,
            isOngoing = sbn.isOngoing,
            isIncomingCall = isIncomingCall
        )

        processor.process(notificationData)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Ignored for MVP
    }
}
