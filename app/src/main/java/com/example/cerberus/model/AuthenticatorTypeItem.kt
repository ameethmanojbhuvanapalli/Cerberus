package com.example.cerberus.model

import com.example.cerberus.auth.authenticator.AuthenticatorType

data class AuthenticatorTypeItem(
    val type: AuthenticatorType,
    val iconRes: Int,
    val displayName: String
)