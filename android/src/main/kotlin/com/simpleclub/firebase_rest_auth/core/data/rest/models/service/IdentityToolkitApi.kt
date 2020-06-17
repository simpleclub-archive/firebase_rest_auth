package com.simpleclub.android.core.data.rest.models.service

import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInAnonymouslyRequest
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInWithCustomTokenRequest
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInWithCustomTokenResponse
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInWithEmailRequest
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInWithEmailResponse
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignInAnonymouslyResponse
import com.simpleclub.android.core.data.rest.models.identitytoolkit.SignUpWithEmailResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Implementation of IdentityToolkit API
 * See also https://firebase.google.com/docs/reference/rest/auth
 */
interface IdentityToolkitApi {

    @POST("v1/accounts:signUp")
    fun signInAnonymously(@Body request: SignInAnonymouslyRequest): Call<SignInAnonymouslyResponse>

    @POST("v1/accounts:signInWithCustomToken")
    fun signInWithCustomToken(@Body request: SignInWithCustomTokenRequest): Call<SignInWithCustomTokenResponse>

    @POST("v1/accounts:signInWithPassword")
    fun signInWithPassword(@Body request: SignInWithEmailRequest): Call<SignInWithEmailResponse>

    @POST("v1/accounts:signUp")
    fun signUpWithEmail(@Body request: SignInWithEmailRequest): Call<SignUpWithEmailResponse>

}
