package com.simpleclub.android.core.data.source

import com.google.android.gms.tasks.Task
import com.simpleclub.android.core.domain.user.AuthUser

interface AuthDataSource {
	fun addAuthStateListener(authStateListener: AuthStateListener)
	fun removeAuthStateListener(authStateListener: AuthStateListener)
	fun signInWithCustomToken(token: String): Task<*>
	fun signInWithEmail(email: String, password: String): Task<*>
	fun signInWithCredential(credential: Any): Task<*>
	fun signOut()
	fun getUser(): AuthUser?

	suspend fun getIdToken(): String?

	interface AuthStateListener {
		fun onAuthStateChanged()
	}
}
