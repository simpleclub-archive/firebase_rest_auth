package com.simpleclub.android.framework.rest

import androidx.annotation.Keep
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.internal.InternalAuthProvider
import com.google.firebase.components.Component
import com.google.firebase.components.ComponentRegistrar
import com.google.firebase.components.Dependency
import com.simpleclub.android.core.data.rest.models.FirebaseRestAuth

/**
 * Required so other Firebase libraries can find this implementation of InternalAuthProvider.
 * Note, you cannot also include the FirebaseAuth client library in your build.
 */
@Keep
class RestAuthRegistrar : ComponentRegistrar {

	override fun getComponents(): MutableList<Component<*>> {
		val restAuthComponent =
				Component.builder(InternalAuthProvider::class.java)
						.add(Dependency.required(FirebaseApp::class.java))
						.factory { container ->
							val firebaseApp = container.get(FirebaseApp::class.java)
							return@factory FirebaseRestAuth.getInstance(firebaseApp)
						}
						.build()

		return mutableListOf(restAuthComponent)
	}

}
