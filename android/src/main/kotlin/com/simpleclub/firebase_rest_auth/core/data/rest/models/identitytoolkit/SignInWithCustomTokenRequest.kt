package com.simpleclub.android.core.data.rest.models.identitytoolkit

data class SignInWithCustomTokenRequest(
    val token: String,
    val returnSecureToken: Boolean = true
)
