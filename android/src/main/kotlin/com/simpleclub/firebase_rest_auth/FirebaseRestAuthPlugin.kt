// Copyright 2017 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.simpleclub.firebase_rest_auth

import android.app.Activity
import android.content.Context
import android.util.SparseArray
import com.google.firebase.FirebaseApp
import com.simpleclub.android.core.data.rest.models.FirebaseRestAuth
import com.simpleclub.android.core.data.rest.models.FirebaseRestAuth.Companion.getInstance
import com.simpleclub.android.core.data.source.AuthDataSource.AuthStateListener
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar

/**
 * Flutter plugin for Firebase Auth.
 */
class FirebaseRestAuthPlugin : MethodCallHandler, FlutterPlugin, ActivityAware {

  private var authStateListeners: SparseArray<AuthStateListener>? = null

  // private SparseArray<ForceResendingToken> forceResendingTokens;
  private var channel: MethodChannel? = null

  // Only set activity for v2 embedder. Always access activity from getActivity() method.
  private var activity: Activity? = null

  // Handles are ints used as indexes into the sparse array of active observers
  private val nextHandle = 0
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

  private fun getAuth(call: MethodCall): FirebaseRestAuth {
    val arguments = call.arguments<Map<String, Any>>()
    val appName = arguments["app"] as String?
    val app = FirebaseApp.getInstance(appName!!)
    return getInstance(app)
  }

  override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
    when (call.method) {
      else -> result.notImplemented()
    }
  }
}