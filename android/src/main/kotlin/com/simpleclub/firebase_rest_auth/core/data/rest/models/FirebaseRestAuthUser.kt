package com.simpleclub.firebase_rest_auth.core.data.rest.models

import android.util.Log
import com.simpleclub.firebase_rest_auth.framework.rest.utils.IdTokenParser

@Suppress("UNCHECKED_CAST")
class FirebaseRestAuthUser(
		val idToken: String,
		val refreshToken: String
) {
	val isAnonymous: Boolean
	val userId: String
	val expirationTime: Long
	val name: String?
	val picture: String?
	val email: String?
	val emailVerified: Boolean?
	val providerId: String?
	val providerInfo: Map<String, Any>?

	init {
		val claims = IdTokenParser.parseIdToken(this.idToken)

		this.userId = claims["user_id"].toString()
		this.expirationTime = claims["exp"].toString().toLong()
		this.name = claims["name"]?.toString()
		this.picture = claims["picture"]?.toString()
		this.email = claims["email"]?.toString()
		this.emailVerified = claims["email_verified"]?.toString()?.toBoolean()

		val firebase = claims["firebase"] as? Map<String, Any>
		val providerId = firebase?.get("sign_in_provider")?.toString()

		this.providerId = providerId
		this.providerInfo = firebase?.get("identities") as? Map<String, Any>
		this.isAnonymous = providerId == null || providerId == "anonymous"

	}

	override fun toString(): String {
		return "FirebaseRestAuthUser(idToken='[REDACTED]', refreshToken='[REDACTED]', isAnonymous=$isAnonymous, userId='$userId', expirationTime=$expirationTime, name='$name', picture='$picture', email='$email', emailVerified=$emailVerified, providerId='$providerId', providerInfo=$providerInfo)"
	}
}
