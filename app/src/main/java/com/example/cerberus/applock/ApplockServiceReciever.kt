package com.example.cerberus.applock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AppLockServiceControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            "com.example.cerberus.START_LOCK_SERVICE" -> {
                ApplockManager.start(context)
            }
            "com.example.cerberus.STOP_LOCK_SERVICE" -> {
                ApplockManager.stop(context)
            }
        }
    }
}
