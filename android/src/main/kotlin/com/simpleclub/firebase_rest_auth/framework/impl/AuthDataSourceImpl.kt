package com.simpleclub.android.framework.impl

import android.app.Activity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.internal.IdTokenListener
import com.simpleclub.android.core.data.rest.models.FirebaseRestAuth
import com.simpleclub.android.core.data.source.AuthDataSource
import com.simpleclub.android.core.domain.user.AuthUser

class AuthDataSourceImpl : AuthDataSource {

	private val mRestAuth = FirebaseRestAuth.getInstance(FirebaseApp.getInstance())
	private val idTokenListeners = mutableMapOf<AuthDataSource.AuthStateListener, IdTokenListener>()

	override fun addAuthStateListener(authStateListener: AuthDataSource.AuthStateListener) {
		idTokenListeners[authStateListener] = IdTokenListener {
			authStateListener.onAuthStateChanged()
		}
		mRestAuth.addIdTokenListener(idTokenListeners[authStateListener]!!)
	}

	override fun removeAuthStateListener(authStateListener: AuthDataSource.AuthStateListener) {
		idTokenListeners[authStateListener]?.let { mRestAuth.removeIdTokenListener(it) }
	}

	override fun signInWithCustomToken(token: String): Task<*> {
		return mRestAuth.signInWithCustomToken(token)
	}

	override fun signInWithEmail(email: String, password: String): Task<*> {
		return mRestAuth.signInWithEmail(email, password)
	}

	override fun signInWithCredential(credential: Any): Task<*> {
		throw IllegalStateException("implementation not supported with RestAuth")
	}

	override fun signOut() {
		mRestAuth.signOut()
	}

	override fun getUser(): AuthUser? {
		return mRestAuth.currentUser?.let {
			AuthUser(
					uid = it.userId,
					isAnonymous = it.isAnonymous,
					providerId = "hms"
			)
		}
	}

	override suspend fun getIdToken(): String? {
		return mRestAuth.currentUser?.idToken
	}
}
