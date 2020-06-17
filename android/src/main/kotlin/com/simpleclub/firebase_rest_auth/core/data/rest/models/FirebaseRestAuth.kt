package com.simpleclub.android.core.data.rest.models

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.internal.InternalAuthProvider
import com.simpleclub.android.framework.rest.FirebaseTokenRefresher
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInWithCustomTokenResponse
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInWithEmailResponse
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInAnonymouslyResponse
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignUpWithEmailResponse
import com.simpleclub.android.framework.rest.RestAuthProviderImpl

interface FirebaseRestAuth : InternalAuthProvider {

    val tokenRefresher: FirebaseTokenRefresher
    var currentUser: FirebaseRestAuthUser?

    fun signInAnonymously(): Task<SignInAnonymouslyResponse>
    fun signInWithCustomToken(token: String): Task<SignInWithCustomTokenResponse>
    fun signInWithEmail(email: String, password: String): Task<SignInWithEmailResponse>
    fun signUpWithEmail(email: String, password: String): Task<SignUpWithEmailResponse>
    fun signOut()

    companion object {
        private val INSTANCE = mutableMapOf<String, RestAuthProviderImpl>()

        fun getInstance(app: FirebaseApp): FirebaseRestAuth {
            if (!INSTANCE.containsKey(app.name)) {
                val instance = RestAuthProviderImpl(app)
                INSTANCE[app.name] = instance
            }

            return INSTANCE[app.name]!!
        }
    }
}

