package ink.blavingad.hcb.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ink.blavingad.hcb.MainActivity
import ink.blavingad.hcb.R
import ink.blavingad.hcb.container
import ink.blavingad.hcb.ui.money
import ink.blavingad.hcb.ui.title
import java.util.concurrent.TimeUnit

/** HCB has no push for third-party apps: poll every 15 min (Android minimum) and notify new transactions. */
class TxWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        if (c.auth.tokens.value == null || !c.prefs.notify.value) return Result.success()
        val sp = applicationContext.getSharedPreferences("seen", Context.MODE_PRIVATE)
        val first = !sp.contains("ids")
        val seen = sp.getStringSet("ids", emptySet())!!.toMutableSet()
        val fresh = mutableListOf<ink.blavingad.hcb.data.Transaction>()
        try {
            for (o in c.api.organizations()) {
                val txs = c.api.orgTransactions(o.id, null, null, null).data.take(10)
                txs.forEach { t -> if (seen.add(t.id) && !first) fresh += t.copy(organization = t.organization ?: o) }
            }
        } catch (_: Exception) { return Result.retry() }
        sp.edit { putStringSet("ids", seen.toList().takeLast(500).toSet()) }
        fresh.take(5).forEach { notify(applicationContext, it) }
        return Result.success()
    }

    companion object {
        const val CHANNEL = "transactions"
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<TxWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork("tx-poll", ExistingPeriodicWorkPolicy.KEEP, req)
        }

        private fun channel(ctx: Context) {
            if (Build.VERSION.SDK_INT >= 26) ctx.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(NotificationChannel(CHANNEL, "Transactions", NotificationManager.IMPORTANCE_DEFAULT))
        }

        private fun intent(ctx: Context, t: ink.blavingad.hcb.data.Transaction, mode: String, code: Int) = PendingIntent.getActivity(
            ctx, code,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("receipt_tx", t.id).putExtra("receipt_org", t.organization?.id).putExtra("receipt_mode", mode),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun notify(ctx: Context, t: ink.blavingad.hcb.data.Transaction) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            channel(ctx)
            val base = t.id.hashCode() and 0x0fffffff
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("${money(t.amount_cents, signed = true)} · ${t.title()}")
                .setContentText(t.organization?.name ?: "New transaction")
                .setAutoCancel(true)
                .setContentIntent(intent(ctx, t, if (t.missing_receipt) "camera" else "open", base))
                .apply {
                    if (t.amount_cents < 0) {
                        addAction(0, "Photo", intent(ctx, t, "camera", base + 1))
                        addAction(0, "Gallery", intent(ctx, t, "file", base + 2))
                    }
                }.build()
            NotificationManagerCompat.from(ctx).notify(base, n)
        }
    }
}
