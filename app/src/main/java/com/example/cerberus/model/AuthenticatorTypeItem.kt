package com.example.cerberus.model

import com.example.cerberus.auth.AuthenticatorType

data class AuthenticatorTypeItem(
    val type: AuthenticatorType,
    val iconRes: Int,
    val displayName: String
)