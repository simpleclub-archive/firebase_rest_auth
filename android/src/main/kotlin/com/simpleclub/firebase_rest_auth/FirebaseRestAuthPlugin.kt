// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.simpleclub.firebase_rest_auth

import android.app.Activity
import android.util.Log
import androidx.annotation.Nullable
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.internal.IdTokenListener
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
import io.flutter.plugins.firebase.core.FlutterFirebasePluginRegistry.registerPlugin
import okhttp3.internal.toImmutableList
import okhttp3.internal.toImmutableMap
import java.util.concurrent.Callable


/**
 * Flutter plugin for Firebase Auth. Uses REST APIs instead of GMS.
 * Original class: [FirebaseAuthPlugin](https://github.com/FirebaseExtended/flutterfire/blob/master/packages/firebase_auth/firebase_auth/android/src/main/java/io/flutter/plugins/firebaseauth/FirebaseAuthPlugin.java)
 */
class FirebaseRestAuthPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {

	private var authStateListeners: MutableMap<String, AuthStateListener>? = null
	private var idTokenListeners: MutableMap<String, IdTokenListener>? = null

	// private SparseArray<ForceResendingToken> forceResendingTokens;
	private var channel: MethodChannel? = null

	// Only set activity for v2 embedder. Always access activity from getActivity() method.
	private var activity: Activity? = null

	// Handles are ints used as indexes into the sparse array of active observers
	private var nextHandle = 0

	private fun initInstance(messenger: BinaryMessenger) {
		val channelName = "plugins.flutter.io/firebase_auth"
		channel = MethodChannel(messenger, channelName)
		channel!!.setMethodCallHandler(this)

		authStateListeners = HashMap()
		idTokenListeners = HashMap()
	}

	private fun getMethodChannelResultHandler(method: String): MethodChannel.Result? {
		return object : MethodChannel.Result {
			override fun success(@Nullable result: Any?) {
				// Noop
			}

			override fun notImplemented() {
				Log.e(Constants.TAG, "$method has not been implemented")
			}

			override fun error(errorCode: String?, errorMessage: String?, errorDetails: Any?) {
				Log.e(Constants.TAG, "$method error ($errorCode): $errorMessage")
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

	// Ensure any listeners are removed when the app
	// is detached from the FlutterEngine
	private fun removeEventListeners() {
		authStateListeners?.keys?.forEach {
			authStateListeners?.get(it)?.let { listener -> getAuth(mapOf(Constants.APP_NAME to it)).removeAuthStateListener(listener) }
		}
		idTokenListeners?.keys?.forEach {
			idTokenListeners?.get(it)?.let { listener -> getAuth(mapOf(Constants.APP_NAME to it)).removeIdTokenListener(listener) }
		}
		authStateListeners = null;
		idTokenListeners = null;
	}

	private fun getAuth(arguments: Map<String, Any>): AuthDataSource {
		val appName = arguments[Constants.APP_NAME] as String
		val app = FirebaseApp.getInstance(appName)
		return AuthDataSource.getInstance(app)
	}

	override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
		Log.d(this.javaClass.toString(), "Method call: ${call.method}")

		when (call.method) {
			"Auth#registerChangeListeners" -> registerChangeListeners(call.arguments())
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

	@Suppress("UNCHECKED_CAST")
	private fun handleSignInWithCredential(call: MethodCall, result: MethodChannel.Result, auth: AuthDataSource) {
		// {app=[DEFAULT], data={password=, email=}, provider=password}
		val credential = call.arguments<Any>() as Map<String?, Any?>
		if (credential["provider"] != "password") {
			result.notImplemented()
			return
		}
		val data = credential["data"] as Map<String?, Any?>
		val email = data["email"]
		val password = data["password"]
		if (email !is String || password !is String) {
			result.error("NO_CREDENTIALS_PROVIDED", "email or password not found", null)
			return
		}
		auth.signInWithEmail(email, password).addOnCompleteListener(SignInCompleteListener(result, auth))
	}

	private fun handleSignInWithCustomToken(call: MethodCall, result: MethodChannel.Result, auth: AuthDataSource) {
		val arguments = call.arguments<Map<String, String>>()
		val token = arguments["token"]
		if (token == null) {
			result.error("404", "no custom token found", "token argument was null")
			return
		}
		auth.signInWithCustomToken(token).addOnCompleteListener(SignInCompleteListener(result, auth))
	}


	private fun handleSignOut(call: MethodCall, result: MethodChannel.Result, auth: AuthDataSource) {
		auth.signOut()
		result.success(null)
	}

	private fun registerChangeListeners(arguments: Map<String, Any>): Task<Void?> {
		return Tasks.call(
				cachedThreadPool,
				Callable<Void> {
					val appName = arguments[Constants.APP_NAME] as String
					val firebaseAuth: AuthDataSource = getAuth(arguments)

					val authStateListener: AuthStateListener? = authStateListeners?.get(appName)
					val idTokenListener: IdTokenListener? = idTokenListeners?.get(appName)

					val event: MutableMap<String, Any?> = HashMap()
					event[Constants.APP_NAME] = appName

					if (authStateListener == null) {
						val newAuthStateListener = object : AuthStateListener {
							override fun onAuthStateChanged() {
								val user: AuthUser? = firebaseAuth.getUser()
								if (user == null) {
									event[Constants.USER] = null
								} else {
									event[Constants.USER] = parseFirebaseUser(user)
								}

								channel!!.invokeMethod("Auth#authStateChanges", event, getMethodChannelResultHandler("Auth#authStateChanges"))
							}
						}
						firebaseAuth.addAuthStateListener(newAuthStateListener)
						authStateListeners?.put(appName, newAuthStateListener)
					}
					if (idTokenListener == null) {
						val newIdTokenChangeListener = IdTokenListener { auth ->
							val user: AuthUser? = firebaseAuth.getUser()
							if (user == null) {
								event[Constants.USER] = null
							} else {
								event[Constants.USER] = parseFirebaseUser(user)
							}
							channel!!.invokeMethod(
									"Auth#idTokenChanges",
									event,
									getMethodChannelResultHandler("Auth#idTokenChanges"))
						}
						firebaseAuth.addIdTokenListener(newIdTokenChangeListener)
						idTokenListeners?.put(appName, newIdTokenChangeListener)
					}
					null
				})
	}

	companion object {

		private fun parseFirebaseUser(firebaseUser: AuthUser?): Map<String, Any?>? {
			if (firebaseUser == null) {
				return null
			}
			val output: MutableMap<String, Any?> = HashMap()
			output[Constants.DISPLAY_NAME] = firebaseUser.name
			output[Constants.EMAIL] = firebaseUser.email
			output[Constants.EMAIL_VERIFIED] = firebaseUser.emailVerified
			output[Constants.IS_ANONYMOUS] = firebaseUser.isAnonymous
			output[Constants.PHOTO_URL] = firebaseUser.picture
			output[Constants.PROVIDER_DATA] = parseUserInfoList(firebaseUser.providerInfo)
			output[Constants.UID] = firebaseUser.uid
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
	}

	private class SignInCompleteListener<T> internal constructor(private val result: MethodChannel.Result, private val auth: AuthDataSource) : OnCompleteListener<T?> {
		override fun onComplete(task: Task<T?>) {
			if (!task.isSuccessful || task.result == null) {
				val exception = task.exception!!
				result.error(exception::class.java.simpleName, exception.localizedMessage, null);
			} else {
				val user: AuthUser? = auth.getUser()
				val userMap: Map<String, Any?>? = parseFirebaseUser(user)
				val map: MutableMap<String, Any?> = mutableMapOf()
				map["user"] = userMap
				result.success(map.toImmutableMap())
			}
		}
	}
}