// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.simpleclub.firebase_rest_auth

import android.app.Activity
import android.content.Context
import android.util.SparseArray
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
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
import okhttp3.internal.toImmutableList
import okhttp3.internal.toImmutableMap


/**
 * Flutter plugin for Firebase Auth. Uses REST APIs instead of GMS.
 * Original class: [FirebaseAuthPlugin](https://github.com/FirebaseExtended/flutterfire/blob/master/packages/firebase_auth/firebase_auth/android/src/main/java/io/flutter/plugins/firebaseauth/FirebaseAuthPlugin.java)
 */
class FirebaseRestAuthPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {

	private var authStateListeners: SparseArray<AuthStateListener>? = null

	// private SparseArray<ForceResendingToken> forceResendingTokens;
	private var channel: MethodChannel? = null

	// Only set activity for v2 embedder. Always access activity from getActivity() method.
	private var activity: Activity? = null

	// Handles are ints used as indexes into the sparse array of active observers
	private var nextHandle = 0

	private fun initInstance(messenger: BinaryMessenger, context: Context) {
		channel = MethodChannel(messenger, "plugins.flutter.io/firebase_auth")
		FirebaseApp.initializeApp(context)
		channel!!.setMethodCallHandler(this)
		authStateListeners = SparseArray()
		// forceResendingTokens = new SparseArray<>();
	}

	// Only access activity with this method.
	fun getActivity(): Activity {
		return activity!!
	}

	override fun onAttachedToEngine(binding: FlutterPluginBinding) {
		initInstance(binding.binaryMessenger, binding.applicationContext)
	}

	override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
		authStateListeners = null
		// forceResendingTokens = null;
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

	private fun getAuth(call: MethodCall): AuthDataSource {
		val arguments = call.arguments<Map<String, Any>>()
		val appName = arguments["app"] as String?
		val app = FirebaseApp.getInstance(appName!!)
		return AuthDataSource.getInstance(app)
	}

	override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
		val auth = getAuth(call)
		when (call.method) {
			"currentUser" -> handleCurrentUser(call, result, auth)
			"startListeningAuthState" -> handleStartListeningAuthState(call, result, auth)
			"signInWithCustomToken" -> handleSignInWithCustomToken(call, result, auth)
			"signInWithCredential" -> handleSignInWithCredential(call, result, auth)
			"signOut" -> handleSignOut(call, result, auth)
			else -> result.notImplemented()
		}
	}

	private fun handleCurrentUser(call: MethodCall, result: MethodChannel.Result, auth: AuthDataSource) {
		val user = auth.getUser()
		if (user == null) {
			result.success(null)
			return
		}
		val userMap = mapFromUser(user)
		result.success(userMap)
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

	private fun handleStartListeningAuthState(call: MethodCall, result: MethodChannel.Result, auth: AuthDataSource) {
		val handle = nextHandle++
		val listener = object : AuthStateListener {
			override fun onAuthStateChanged() {
				val user: AuthUser? = auth.getUser()
				val userMap: Map<String, Any>? = mapFromUser(user)
				val map: MutableMap<String, Any> = mutableMapOf()
				map["id"] = handle
				if (userMap != null) {
					map["user"] = userMap
				}
				channel!!.invokeMethod("onAuthStateChanged", map.toImmutableMap())
			}
		}

		getAuth(call).addAuthStateListener(listener)
		authStateListeners?.append(handle, listener)
		result.success(handle)
	}

	private fun handleSignOut(call: MethodCall, result: MethodChannel.Result, auth: AuthDataSource) {
		auth.signOut()
		result.success(null)
	}

	companion object {

		private fun mapFromUser(user: AuthUser?): Map<String, Any>? {
			return if (user != null) {
				val providerData: MutableList<Map<String, Any?>> = mutableListOf()
				user.providerInfo?.keys?.forEach { providerKey ->
					providerData.add(mapOf(
							"providerId" to providerKey,
							"uid" to (user.providerInfo.getValue(providerKey) as? List<*>)?.get(0)
					))
				}
				val userMap = userInfoToMap(user)

				userMap["isAnonymous"] = user.isAnonymous
				userMap["providerData"] = providerData.toImmutableList()
				return userMap.toImmutableMap()
			} else {
				null
			}
		}


		private fun userInfoToMap(userInfo: AuthUser): MutableMap<String, Any> {
			val map: MutableMap<String, Any> = mutableMapOf()
			map["providerId"] = userInfo.providerId ?: "custom"
			map["uid"] = userInfo.uid
			userInfo.name?.let { map["displayName"] = it }
			userInfo.picture?.let { map["photoUrl"] = it }
			userInfo.email?.let { map["email"] = it }
			userInfo.emailVerified?.let { map["isEmailVerified"] = it }
			return map
		}
	}

	private class SignInCompleteListener<T> internal constructor(private val result: MethodChannel.Result, private val auth: AuthDataSource) : OnCompleteListener<T?> {
		override fun onComplete(task: Task<T?>) {
			if (!task.isSuccessful || task.result == null) {
				val exception = task.exception!!
				result.error(exception::class.java.simpleName, exception.localizedMessage, null);
			} else {
				val user: AuthUser? = auth.getUser()
				val userMap: Map<String, Any>? = mapFromUser(user)
				val map: MutableMap<String, Any?> = mutableMapOf()
				map["user"] = userMap
				result.success(map.toImmutableMap())
			}
		}
	}
}