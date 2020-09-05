// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.simpleclub.firebase_rest_auth

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.Nullable
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.simpleclub.firebase_rest_auth.core.data.rest.models.FirebaseRestAuthUser
import com.simpleclub.firebase_rest_auth.core.data.source.AuthDataSource
import com.simpleclub.firebase_rest_auth.core.data.source.AuthDataSource.AuthStateListener
import com.simpleclub.firebase_rest_auth.core.domain.user.AuthUser
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugins.firebase.core.FlutterFirebasePlugin
import io.flutter.plugins.firebase.core.FlutterFirebasePlugin.cachedThreadPool
import okhttp3.internal.toImmutableList
import java.util.concurrent.Callable


/**
 * Flutter plugin for Firebase Auth. Uses REST APIs instead of GMS.
 * Original class: [FirebaseAuthPlugin](https://github.com/FirebaseExtended/flutterfire/blob/master/packages/firebase_auth/firebase_auth/android/src/main/java/io/flutter/plugins/firebaseauth/FirebaseAuthPlugin.java)
 */
class FirebaseRestAuthPlugin : FlutterFirebasePlugin, MethodCallHandler, FlutterPlugin, ActivityAware {

	private var authStateListeners: MutableMap<String, AuthStateListener>? = null

	// private SparseArray<ForceResendingToken> forceResendingTokens;
	private var channel: MethodChannel? = null

	// Only set activity for v2 embedder. Always access activity from getActivity() method.
	private var activity: Activity? = null

	private fun initInstance(messenger: BinaryMessenger) {
		val channelName = "plugins.flutter.io/firebase_auth"
		channel = MethodChannel(messenger, channelName)
		channel!!.setMethodCallHandler(this)

		authStateListeners = HashMap()
	}

	@Suppress("SameParameterValue")
	private fun getMethodChannelResultHandler(method: String): MethodChannel.Result? {
		return object : MethodChannel.Result {
			override fun success(@Nullable result: Any?) {
				// Noop
			}

			override fun notImplemented() {
				Log.e(TAG, "$method has not been implemented")
			}

			override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
				Log.e(TAG, "$method error ($errorCode): $errorMessage")
			}
		}
	}

	override fun onAttachedToEngine(binding: FlutterPluginBinding) {
		initInstance(binding.binaryMessenger)
	}

	override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
		removeEventListeners()

		channel!!.setMethodCallHandler(null)
		channel = null
	}

	override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
		activity = activityPluginBinding.activity
	}

	override fun onDetachedFromActivityForConfigChanges() {
		activity = null
	}

	override fun onReattachedToActivityForConfigChanges(activityPluginBinding: ActivityPluginBinding) {
		activity = activityPluginBinding.activity
	}

	override fun onDetachedFromActivity() {
		activity = null
	}

	override fun didReinitializeFirebaseCore(): Task<Void> {
		return Tasks.call(
				cachedThreadPool,
				Callable<Void> {
					null
				}
		)
	}

	override fun getPluginConstantsForFirebaseApp(firebaseApp: FirebaseApp?): Task<MutableMap<String, Any>> {
		return Tasks.call(
				cachedThreadPool,
				Callable<MutableMap<String, Any>> {
					val constants = mutableMapOf<String, Any>()

					val auth = firebaseApp?.name?.let { getAuth(mapOf(Constants.APP_NAME to it)) }
					val user = parseFirebaseUser(auth?.getUser())

					if (user != null) {
						constants["APP_CURRENT_USER"] = user
					}

					constants
				}
		)
	}

	// Ensure any listeners are removed when the app
	// is detached from the FlutterEngine
	private fun removeEventListeners() {
		authStateListeners?.keys?.forEach {
			authStateListeners?.get(it)?.let { listener -> getAuth(mapOf(Constants.APP_NAME to it)).removeAuthStateListener(listener) }
		}
		authStateListeners = null
	}

	private fun getAuth(arguments: Map<String, Any>): AuthDataSource {
		val appName = arguments[Constants.APP_NAME] as String
		val app = FirebaseApp.getInstance(appName)
		return AuthDataSource.getInstance(app)
	}

	override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
		Log.d(TAG, "Method call: ${call.method}")

		when (call.method) {
			"Auth#registerChangeListeners" -> registerChangeListeners(call.arguments())
			"Auth#signInWithCustomToken" -> signInWithCustomToken(call.arguments())
			"Auth#signInAnonymously" -> signInAnonymously(call.arguments())
			"Auth#signOut" -> signOut(call.arguments())
			"User#getIdToken" -> getIdToken(call.arguments())
			else -> {
				result.notImplemented()
				return
			}
		}.addOnCompleteListener { task ->
			if (task.isSuccessful) {
				result.success(task.result)
			} else {
				val exception = task.exception
				result.error(
						"firebase_auth",
						exception?.message,
						exception
				)
			}
		}
	}

	private fun signOut(arguments: Map<String, Any>): Task<Void?> {
		return Tasks.call(
				cachedThreadPool,
				Callable<Void?> {
					val auth = getAuth(arguments)
					auth.signOut()
					null
				}
		)
	}

	private fun signInAnonymously(arguments: Map<String, Any>): Task<Map<String, Any?>> {
		return Tasks.call(
				cachedThreadPool,
				Callable {
					val auth = getAuth(arguments)
					val response = Tasks.await(auth.signInAnonymously())
					auth.setUser(FirebaseRestAuthUser(response.idToken, response.refreshToken))
					parseAuthResult(auth.getUser())
				}
		)
	}

	private fun signInWithCustomToken(arguments: Map<String, Any>): Task<Map<String, Any?>> {
		return Tasks.call(
				cachedThreadPool,
				Callable {
					val auth = getAuth(arguments)
					val response = Tasks.await(auth.signInWithCustomToken(arguments[Constants.TOKEN] as String))
					auth.setUser(FirebaseRestAuthUser(response.idToken, response.refreshToken))
					parseAuthResult(auth.getUser())
				}
		)
	}

	private fun registerChangeListeners(arguments: Map<String, Any>): Task<Void?> {
		return Tasks.call(
				cachedThreadPool,
				Callable<Void> {
					val appName = arguments[Constants.APP_NAME] as String
					val auth: AuthDataSource = getAuth(arguments)

					var authStateListener: AuthStateListener? = authStateListeners?.get(appName)

					val event: MutableMap<String, Any?> = HashMap()
					event[Constants.APP_NAME] = appName

					if (authStateListener == null) {
						authStateListener = object : AuthStateListener {
							override fun onAuthStateChanged() {
								val user: AuthUser? = auth.getUser()
								if (user == null) {
									event[Constants.USER] = null
								} else {
									event[Constants.USER] = parseFirebaseUser(user)
								}

								Handler(Looper.getMainLooper()).post {
									channel!!.invokeMethod("Auth#idTokenChanges", event, getMethodChannelResultHandler("Auth#idTokenChanges"))
									channel!!.invokeMethod("Auth#authStateChanges", event, getMethodChannelResultHandler("Auth#authStateChanges"))
								}
							}
						}
						auth.addAuthStateListener(authStateListener)
						authStateListeners?.put(appName, authStateListener)
					}
					FirebaseRestAuthPlugin
					if (auth.getUser() != null) authStateListener.onAuthStateChanged()

					null
				}
		)
	}

	private fun getIdToken(arguments: Map<String, Any>): Task<Map<String, Any?>> {
		return Tasks.call(
				cachedThreadPool,
				Callable call@{
					val auth = getAuth(arguments);
					// val forceRefresh = arguments[Constants.FORCE_REFRESH] as? Boolean ?: false
					val tokenOnly = arguments[Constants.TOKEN_ONLY] as Boolean
					if (auth.getUser() == null) {
						throw IllegalStateException("Login before requesting the IdToken")
					}
					val token: Any? = auth.getIdToken();
					if (tokenOnly) {
						return@call mapOf(
								Constants.TOKEN to token
						);
					} else {
						throw UnsupportedOperationException("Only token only requests are supported")
					}
				})
	}

	companion object {

		private fun parseFirebaseUser(user: AuthUser?): Map<String, Any?>? {
			if (user == null) {
				return null
			}
			val output: MutableMap<String, Any?> = HashMap()
			output[Constants.DISPLAY_NAME] = user.name
			output[Constants.EMAIL] = user.email
			output[Constants.EMAIL_VERIFIED] = user.emailVerified
			output[Constants.IS_ANONYMOUS] = user.isAnonymous
			output[Constants.PHOTO_URL] = user.picture
			output[Constants.PROVIDER_DATA] = parseUserInfoList(user.providerInfo)
			output[Constants.UID] = user.uid
			return output
		}

		private fun parseUserInfoList(providerInfo: Map<String, Any>?): List<Map<String, Any?>> {
			val providerData: MutableList<Map<String, Any?>> = mutableListOf()
			providerInfo?.keys?.forEach { providerKey ->
				providerData.add(mapOf(
						Constants.PROVIDER_ID to providerKey,
						Constants.UID to (providerInfo[providerKey] as? List<*>)?.get(0)
				))
			}
			return providerData.toImmutableList()
		}

		private fun parseAuthResult(user: AuthUser?): Map<String, Any?> {
			return mapOf(
					Constants.USER to parseFirebaseUser(user)
			)
		}

		private val TAG = FirebaseRestAuthPlugin::class.java.simpleName
	}

}