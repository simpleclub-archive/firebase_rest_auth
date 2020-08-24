package com.simpleclub.firebase_rest_auth.core.data.source

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.internal.IdTokenListener
import com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit.SignInWithCustomTokenRequest
import com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit.SignInWithCustomTokenResponse
import com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit.SignInWithEmailResponse
import com.simpleclub.firebase_rest_auth.core.domain.user.AuthUser
import com.simpleclub.firebase_rest_auth.framework.impl.AuthDataSourceImpl

interface AuthDataSource {
	fun addAuthStateListener(authStateListener: AuthStateListener)
	fun removeAuthStateListener(authStateListener: AuthStateListener)
	fun signInWithCustomToken(token: String): Task<SignInWithCustomTokenResponse>
	fun signInWithEmail(email: String, password: String): Task<SignInWithEmailResponse>
	fun signInWithCredential(credential: Any): Task<*>
	fun signOut()
	fun getUser(): AuthUser?

	suspend fun getIdToken(): String?

	companion object {
		private val INSTANCE = mutableMapOf<String, AuthDataSource>()

		fun getInstance(app: FirebaseApp): AuthDataSource {
			if (!INSTANCE.containsKey(app.name)) {
				val instance = AuthDataSourceImpl(app)
				INSTANCE[app.name] = instance
			}

			return INSTANCE[app.name]!!
		}
	}

	interface AuthStateListener {
		fun onAuthStateChanged()
	}
}
