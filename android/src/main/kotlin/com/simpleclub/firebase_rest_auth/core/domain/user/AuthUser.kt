package com.simpleclub.firebase_rest_auth.core.domain.user

data class AuthUser(
		val uid: String,
		val isAnonymous: Boolean,
		val name: String?,
		val picture: String?,
		val email: String?,
		val emailVerified: Boolean?,
		val providerId: String?,
		val providerInfo: Map<String, Any>?
)
