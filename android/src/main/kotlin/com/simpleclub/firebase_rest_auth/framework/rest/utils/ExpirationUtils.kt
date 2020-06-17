package com.simpleclub.android.framework.rest.utils

import com.simpleclub.android.core.data.rest.models.FirebaseRestAuthUser
import java.util.*

class ExpirationUtils {
    companion object {
        fun isExpired(user: FirebaseRestAuthUser): Boolean {
            return expiresInSeconds(user) <= 0
        }

        fun expiresInSeconds(user: FirebaseRestAuthUser): Long {
            val now = Date().time / 1000
            return user.expirationTime - now
        }
    }
}
