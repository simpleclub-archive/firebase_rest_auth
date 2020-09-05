package com.simpleclub.firebase_rest_auth.framework.rest

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.auth.internal.IdTokenListener
import com.google.firebase.internal.InternalTokenResult
import com.google.firebase.internal.api.FirebaseNoSignedInUserException
import com.google.firebase.nongmsauth.api.service.FirebaseKeyInterceptor
import com.simpleclub.firebase_rest_auth.core.data.rest.models.FirebaseRestAuth
import com.simpleclub.firebase_rest_auth.core.data.rest.models.FirebaseRestAuthUser
import com.simpleclub.firebase_rest_auth.core.data.rest.models.identitytoolkit.*
import com.simpleclub.firebase_rest_auth.core.data.rest.models.securetoken.ExchangeTokenRequest
import com.simpleclub.firebase_rest_auth.core.data.rest.models.securetoken.ExchangeTokenResponse
import com.simpleclub.firebase_rest_auth.core.data.rest.models.service.IdentityToolkitApi
import com.simpleclub.firebase_rest_auth.core.data.rest.models.service.SecureTokenApi
import com.simpleclub.firebase_rest_auth.framework.rest.utils.ExpirationUtils
import com.simpleclub.firebase_rest_auth.framework.rest.utils.IdTokenParser
import com.simpleclub.firebase_rest_auth.framework.rest.utils.RetrofitUtils
import com.simpleclub.firebase_rest_auth.framework.rest.utils.UserStorage
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Implementation of FirebaseRestAuth
 * @param app FirebaseApp
 * @param apiKey Web API Key from Firebase Console, usually provided by google-services.json, but there are some cases where this does not match your assigned Web API Key and you will need to override
 */
class RestAuthProviderImpl(app: FirebaseApp, apiKey: String = app.options.apiKey) : FirebaseRestAuth {

	private val context = app.applicationContext
	private val userStorage = UserStorage(context, app)
	private val listeners = mutableListOf<IdTokenListener>()
	private val firebaseApi: IdentityToolkitApi
	private val secureTokenApi: SecureTokenApi

	override val tokenRefresher = FirebaseTokenRefresher(this)

	override var currentUser: FirebaseRestAuthUser? = null
		set(value) {
			Log.d(TAG, "currentUser = $value")

			// Set the local field
			field = value

			// Set the value in persistence
			userStorage.set(value)

			listeners.forEach { listener ->
				listener.onIdTokenChanged(InternalTokenResult(value?.idToken))
			}
		}

	init {
		val loggingInterceptor = HttpLoggingInterceptor()
		loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

		// OkHttpClient with the custom interceptor
		val client = OkHttpClient.Builder()
				.addInterceptor(loggingInterceptor)
				.addInterceptor(FirebaseKeyInterceptor(apiKey))
				.build()

		this.firebaseApi = provideIdentityToolkitApi(client)
		this.secureTokenApi = provideSecureTokenApi(client)

		// TODO: What if the persisted user is expired?
		this.currentUser = userStorage.get()
	}

	override fun signInAnonymously(): Task<SignInAnonymouslyResponse> {
		return RetrofitUtils.callToTask(
				this.firebaseApi.signInAnonymously(
						SignInAnonymouslyRequest()
				)
		).addOnSuccessListener { res ->
			this.currentUser = FirebaseRestAuthUser(res.idToken, res.refreshToken)
		}.addOnFailureListener { e ->
			Log.e(TAG, "signInAnonymously: failed", e)
			this.currentUser = null
		}
	}

	override fun signInWithCustomToken(token: String): Task<SignInWithCustomTokenResponse> {
		return RetrofitUtils.callToTask(
				this.firebaseApi.signInWithCustomToken(
						SignInWithCustomTokenRequest(token)
				)
		).addOnSuccessListener { res ->
			this.currentUser = FirebaseRestAuthUser(res.idToken, res.refreshToken)
		}.addOnFailureListener { e ->
			Log.e(TAG, "signInWithCustomToken: failed", e)
			this.currentUser = null
		}
	}

	override fun signInWithEmail(email: String, password: String): Task<SignInWithEmailResponse> {
		return RetrofitUtils.callToTask(
				this.firebaseApi.signInWithPassword(
						SignInWithEmailRequest(email, password)
				)
		).addOnSuccessListener { res ->
			this.currentUser = FirebaseRestAuthUser(res.idToken, res.refreshToken)
		}.addOnFailureListener { e ->
			Log.e(TAG, "signInWithEmail: failed", e)
			this.currentUser = null
		}
	}

	override fun signUpWithEmail(email: String, password: String): Task<SignUpWithEmailResponse> {
		return RetrofitUtils.callToTask(
				this.firebaseApi.signUpWithEmail(
						SignInWithEmailRequest(email, password)
				)
		).addOnSuccessListener { res ->
			this.currentUser = FirebaseRestAuthUser(res.idToken, res.refreshToken)
		}.addOnFailureListener { e ->
			Log.e(TAG, "signUpWithEmail: failed", e)
			this.currentUser = null
		}
	}

	override fun signOut() {
		this.currentUser = null
	}

	private fun refreshUserToken(): Task<ExchangeTokenResponse> {
		val refreshToken = this.currentUser?.refreshToken
				?: throw Exception("Can't refresh token, current user has no refresh token")

		val request = ExchangeTokenRequest("refresh_token", refreshToken)
		val call = this.secureTokenApi.exchangeToken(request)

		return RetrofitUtils.callToTask(call)
				.addOnSuccessListener { res ->
					currentUser = FirebaseRestAuthUser(res.id_token, res.refresh_token)
				}
	}

	private fun currentUserToTokenResult(): GetTokenResult {
		val token = this.currentUser!!.idToken
		return GetTokenResult(token, IdTokenParser.parseIdToken(token))
	}

	override fun getUid(): String? {
		Log.d(TAG, "getUid()")
		return this.currentUser?.userId
	}

	override fun getAccessToken(forceRefresh: Boolean): Task<GetTokenResult> {
		val source = TaskCompletionSource<GetTokenResult>()
		val user = this.currentUser

		if (user != null) {
			val needsRefresh = forceRefresh || ExpirationUtils.isExpired(user)
			if (!needsRefresh) {
				// Return the current token, no need to check anything
				source.trySetResult(currentUserToTokenResult())
			} else {
				// Get a new token and then return
				this.refreshUserToken()
						.addOnSuccessListener {
							source.trySetResult(currentUserToTokenResult())
						}
						.addOnFailureListener { e ->
							source.trySetException(e)
						}
			}
		} else {
			// Not yet signed in
			source.trySetException(FirebaseNoSignedInUserException("Please sign in before trying to get a token"))
		}

		return source.task
	}

	/**
	 * Note: the JavaDoc says that we should start proactive token refresh here. In order to have better lifecycle
	 * management, we force the user to manually start a "FirebaseTokenRefresher" instead.
	 */
	override fun addIdTokenListener(listener: IdTokenListener) {
		Log.d(TAG, "addIdTokenListener: $listener")
		listeners.add(listener)
	}

	override fun removeIdTokenListener(listener: IdTokenListener) {
		Log.d(TAG, "removeIdTokenListener $listener")
		listeners.remove(listener)
	}


	private fun provideIdentityToolkitApi(httpClient: OkHttpClient): IdentityToolkitApi {
		// Retrofit client pointed at the Firebase IdentityToolkit API
		return Retrofit.Builder()
				.baseUrl("https://identitytoolkit.googleapis.com/")
				.client(httpClient)
				.addConverterFactory(GsonConverterFactory.create())
				.build()
				.create(IdentityToolkitApi::class.java)
	}

	private fun provideSecureTokenApi(httpClient: OkHttpClient): SecureTokenApi {
		// Retrofit client pointed at the Firebase Auth API
		return Retrofit.Builder()
				.baseUrl("https://securetoken.googleapis.com/")
				.client(httpClient)
				.addConverterFactory(GsonConverterFactory.create())
				.build().create(SecureTokenApi::class.java)
	}

	companion object {
		private val TAG = RestAuthProviderImpl::class.java.simpleName
	}
}
