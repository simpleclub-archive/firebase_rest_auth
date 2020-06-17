package com.simpleclub.android.core.domain.user

data class AuthUser(
		val uid: String,
		val isAnonymous: Boolean,
		var providerId: String
)
