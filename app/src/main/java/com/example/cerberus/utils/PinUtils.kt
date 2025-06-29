package com.example.cerberus.utils

import java.security.MessageDigest

object PinUtils {
    fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}