package com.simpleclub.firebase_rest_auth.framework.impl

import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.internal.IdTokenListener
import com.simpleclub.firebase_rest_auth.core.data.rest.models.FirebaseRestAuth
import com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit.SignInWithCustomTokenResponse
import com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit.SignInWithEmailResponse
import com.simpleclub.firebase_rest_auth.core.data.source.AuthDataSource
import com.simpleclub.firebase_rest_auth.core.domain.user.AuthUser

class AuthDataSourceImpl(app: FirebaseApp) : AuthDataSource {

	private val mRestAuth = FirebaseRestAuth.getInstance(app)
	private val idTokenListeners = mutableMapOf<Any, IdTokenListener>()

	override fun addAuthStateListener(authStateListener: AuthDataSource.AuthStateListener) {
		idTokenListeners[authStateListener] = IdTokenListener {
			authStateListener.onAuthStateChanged()
		}
		mRestAuth.addIdTokenListener(idTokenListeners[authStateListener]!!)
	}

	override fun removeAuthStateListener(authStateListener: AuthDataSource.AuthStateListener) {
		idTokenListeners[authStateListener]?.let { mRestAuth.removeIdTokenListener(it) }
	}

	override fun signInWithCustomToken(token: String): Task<SignInWithCustomTokenResponse> {
		return mRestAuth.signInWithCustomToken(token)
	}

	override fun signInWithEmail(email: String, password: String): Task<SignInWithEmailResponse> {
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
					email = it.email,
					emailVerified = it.emailVerified,
					name = it.name,
					picture = it.picture,
					providerId = it.providerId,
					providerInfo = it.providerInfo
			)
		}
	}

	override suspend fun getIdToken(): String? {
		return mRestAuth.currentUser?.idToken
	}

}
