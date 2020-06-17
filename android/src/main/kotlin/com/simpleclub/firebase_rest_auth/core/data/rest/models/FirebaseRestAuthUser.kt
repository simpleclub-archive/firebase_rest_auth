package com.simpleclub.android.core.data.rest.models

import com.simpleclub.android.framework.rest.utils.IdTokenParser

class FirebaseRestAuthUser(
    val idToken: String,
    val refreshToken: String,
    val isAnonymous: Boolean = false
) {

    val userId: String
    val expirationTime: Long

    init {
        val claims = IdTokenParser.parseIdToken(this.idToken)

        this.userId = claims["user_id"].toString()
        this.expirationTime = claims["exp"].toString().toLong()
    }

    override fun toString(): String {
        return "RestAuthUser(userId=$userId, expiresAt=$expirationTime, isAnonymous=$isAnonymous)"
    }

}
