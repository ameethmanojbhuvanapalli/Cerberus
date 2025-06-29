package com.example.cerberus.utils

import android.content.Context
import com.example.cerberus.service.AuthenticationService

object ApplockManager {
    fun start(context: Context) {
        AuthenticationService.getInstance(context).apply {
            cleanupExpiredEntries()
        }
    }

    fun stop(context: Context) {
        AuthenticationService.getInstance(context).shutdown()
    }
}
