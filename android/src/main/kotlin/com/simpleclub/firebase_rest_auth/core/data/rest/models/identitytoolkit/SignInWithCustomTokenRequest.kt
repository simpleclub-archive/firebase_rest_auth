package com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit

data class SignInWithCustomTokenRequest(
    val token: String,
    val returnSecureToken: Boolean = true
)
