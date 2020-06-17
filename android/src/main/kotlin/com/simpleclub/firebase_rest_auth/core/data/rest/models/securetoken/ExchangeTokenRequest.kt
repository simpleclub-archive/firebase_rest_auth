package com.simpleclub.firebase_rest_auth.core.data.rest.models.securetoken

data class ExchangeTokenRequest(
    var grant_type: String,
    var refresh_token: String
)
