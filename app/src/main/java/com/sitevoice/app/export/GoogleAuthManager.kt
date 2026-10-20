package com.sitevoice.app.export

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GoogleAuthManager {

    val SCOPE_DRIVE = Scope("https://www.googleapis.com/auth/drive")
    val SCOPE_DOCUMENTS = Scope("https://www.googleapis.com/auth/documents")
    const val OAUTH_SCOPE_STRING = "oauth2:https://www.googleapis.com/auth/drive https://www.googleapis.com/auth/documents"

    fun getSignInClient(context: Context, webClientId: String? = null): GoogleSignInClient {
        val gsoBuilder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(SCOPE_DRIVE, SCOPE_DOCUMENTS)

        if (!webClientId.isNullOrBlank()) {
            gsoBuilder.requestIdToken(webClientId)
        }

        return GoogleSignIn.getClient(context, gsoBuilder.build())
    }

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    suspend fun fetchAccessToken(context: Context, account: Account): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val token = GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE_STRING)
                Result.success(token)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        val client = GoogleSignIn.getClient(context, GoogleSignInOptions.DEFAULT_SIGN_IN)
        client.signOut().addOnCompleteListener {
            client.revokeAccess().addOnCompleteListener {
                onComplete()
            }
        }
    }
}
