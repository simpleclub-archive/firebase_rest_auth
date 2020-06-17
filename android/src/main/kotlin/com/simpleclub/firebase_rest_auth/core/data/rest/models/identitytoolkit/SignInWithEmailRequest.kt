package com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit

data class SignInWithEmailRequest(
    val email: String,                      // The email the user is signing in with
    val password: String,                   // The password for the account
    val returnSecureToken: Boolean = true   // Whether or not to return an ID and refresh token. Should always be true.
)
