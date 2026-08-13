package com.outsystems.clerk.sharedsession

import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CallbackContext
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import androidx.activity.ComponentActivity
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.SharedSessionSyncConfig
import com.clerk.api.auth.HostedAuthMode
import com.clerk.api.hostedauth.HostedAuthCancellationException
import com.clerk.api.network.serialization.ClerkResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Production-Ready Cordova Plugin for Clerk Shared Session Sync
 * 
 * Features:
 * - Secure hosted authentication (Account Portal in Custom Tabs browser)
 * - Multi-app session sharing via shared storage
 * - Comprehensive error handling and logging
 * - User and session information retrieval
 * - Automatic session synchronization
 * 
 * Setup Requirements:
 * 1. Enable Native API in Clerk Dashboard (https://dashboard.clerk.com/~/native-applications)
 * 2. Add app namespace and package name to Native applications settings
 * 3. Ensure all participating apps use the same Clerk Publishable Key
 * 4. Sign all participating apps with the same signing certificate
 * 5. Set minimum SDK to 24+ in OutSystems settings
 */
class ClerkPlugin : CordovaPlugin() {

    private var isInitialized = false
    private val TAG = "ClerkPlugin"

    override fun execute(action: String, args: JSONArray, callbackContext: CallbackContext): Boolean {
        return when (action) {
            "initialize" -> {
                val jsKey = if (args.length() > 0 && !args.isNull(0)) args.optString(0) else null
                initializeClerk(jsKey, callbackContext)
                true
            }
            "startHostedAuth" -> {
                val mode = if (args.length() > 0) args.optString(0, "signIn") else "signIn"
                startHostedAuth(mode, callbackContext)
                true
            }
            "startSignInFlow" -> startHostedAuth("signIn", callbackContext).let { true }
            "startSignUpFlow" -> startHostedAuth("signUp", callbackContext).let { true }
            "signOut" -> {
                performSignOut(callbackContext)
                true
            }
            "reloadFromSharedStorage" -> {
                reloadSharedStorage(callbackContext)
                true
            }
            "getCurrentUser" -> {
                getCurrentUser(callbackContext)
                true
            }
            "getSessionToken" -> {
                getSessionToken(callbackContext)
                true
            }
            "isUserSignedIn" -> {
                isUserSignedIn(callbackContext)
                true
            }
            else -> false
        }
    }

    private fun initializeClerk(jsKey: String?, callbackContext: CallbackContext) {
        if (isInitialized) {
            Log.d(TAG, "[INIT] Clerk already initialized")
            callbackContext.success("Clerk is already initialized")
            return
        }

        try {
            Log.d(TAG, "[INIT] Starting initialization...")
            val context = cordova.activity.applicationContext

            val resId = context.resources.getIdentifier("clerk_publishable_key", "string", context.packageName)
            val resourceKey = if (resId != 0) context.getString(resId) else null

            val publishableKey = resourceKey?.takeIf { it.isNotBlank() && !it.startsWith("$") } ?: jsKey

            if (publishableKey.isNullOrBlank()) {
                Log.e(TAG, "[INIT] Missing publishable key")
                callbackContext.error("Clerk Publishable Key is missing. Provide via CLERK_PUBLISHABLE_KEY config or JavaScript parameter.")
                return
            }

            if (!publishableKey.startsWith("pk_")) {
                Log.e(TAG, "[INIT] Invalid key format: $publishableKey")
                callbackContext.error("Invalid Publishable Key format. Must start with 'pk_'")
                return
            }

            CoroutineScope(Dispatchers.Main).launch {
                try {
                    Clerk.initialize(
                        context = context,
                        publishableKey = publishableKey,
                        options = ClerkConfigurationOptions(
                            sharedSessionSync = SharedSessionSyncConfig.enabled
                        )
                    )
                    
                    delay(500)
                    isInitialized = true
                    
                    Log.d(TAG, "[INIT] SUCCESS")
                    callbackContext.success(
                        JSONObject().apply {
                            put("message", "Clerk initialized with Shared Session Sync")
                            put("initialized", true)
                        }.toString()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "[INIT] Exception: ${e.message}", e)
                    callbackContext.error("Failed to initialize Clerk: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[INIT] Outer exception: ${e.message}", e)
            callbackContext.error("Failed to initialize Clerk: ${e.message}")
        }
    }

    private fun startHostedAuth(mode: String, callbackContext: CallbackContext) {
        if (!isInitialized) {
            Log.e(TAG, "[AUTH] Not initialized")
            callbackContext.error("Clerk must be initialized before authentication")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val activity = cordova.activity as? ComponentActivity
                if (activity == null) {
                    Log.e(TAG, "[AUTH] Activity unavailable")
                    callbackContext.error("Activity not available")
                    return@launch
                }

                Log.d(TAG, "[AUTH] Starting $mode flow...")

                val authMode = if (mode == "signUp") HostedAuthMode.SIGN_UP else HostedAuthMode.SIGN_IN

                when (val result = Clerk.auth.startHostedAuth(activity = activity, mode = authMode)) {
                    is ClerkResult.Success -> {
                        Log.d(TAG, "[AUTH] SUCCESS")
                        val user = Clerk.user
                        val response = JSONObject().apply {
                            put("success", true)
                            put("message", "Authentication successful")
                            put("userId", user?.id)
                            put("email", user?.primaryEmailAddress?.emailAddress)
                            put("name", "${user?.firstName ?: ""} ${user?.lastName ?: ""}".trim())
                        }
                        callbackContext.success(response.toString())
                    }
                    is ClerkResult.Failure -> {
                        val message = if (result.throwable is HostedAuthCancellationException) {
                            Log.d(TAG, "[AUTH] Cancelled by user")
                            "Authentication cancelled"
                        } else {
                            Log.e(TAG, "[AUTH] Failed: ${result.throwable?.message}")
                            result.throwable?.message ?: "Authentication failed"
                        }
                        callbackContext.error(message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[AUTH] Exception: ${e.message}", e)
                callbackContext.error("Authentication failed: ${e.message}")
            }
        }
    }

    private fun performSignOut(callbackContext: CallbackContext) {
        if (!isInitialized) {
            Log.e(TAG, "[LOGOUT] Not initialized")
            callbackContext.error("Clerk must be initialized before signing out")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "[LOGOUT] Signing out...")
                Clerk.auth.signOut()
                
                Log.d(TAG, "[LOGOUT] SUCCESS")
                val response = JSONObject().apply {
                    put("success", true)
                    put("message", "Signed out successfully")
                }
                callbackContext.success(response.toString())
            } catch (e: Exception) {
                Log.e(TAG, "[LOGOUT] Exception: ${e.message}", e)
                callbackContext.error("Sign out failed: ${e.message}")
            }
        }
    }

    private fun reloadSharedStorage(callbackContext: CallbackContext) {
        if (!isInitialized) {
            Log.e(TAG, "[RELOAD] Not initialized")
            callbackContext.error("Clerk must be initialized before reloading storage")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "[RELOAD] Reloading shared storage...")
                val stateChanged = Clerk.reloadFromSharedStorage()
                
                Log.d(TAG, "[RELOAD] State changed: $stateChanged")
                val response = JSONObject().apply {
                    put("stateChanged", if (stateChanged) 1 else 0)
                    put("message", if (stateChanged) "Session updated" else "No changes")
                }
                callbackContext.success(response.toString())
            } catch (e: Exception) {
                Log.e(TAG, "[RELOAD] Exception: ${e.message}", e)
                callbackContext.error("Failed to reload: ${e.message}")
            }
        }
    }

    private fun getCurrentUser(callbackContext: CallbackContext) {
        if (!isInitialized) {
            Log.e(TAG, "[USER] Not initialized")
            callbackContext.error("Clerk must be initialized")
            return
        }

        try {
            val user = Clerk.user
            Log.d(TAG, "[USER] Current user: ${user?.id ?: "none"}")
            
            val response = JSONObject().apply {
                if (user != null) {
                    put("user", JSONObject().apply {
                        put("id", user.id)
                        put("email", user.primaryEmailAddress?.emailAddress)
                        put("firstName", user.firstName)
                        put("lastName", user.lastName)
                        put("name", "${user.firstName ?: ""} ${user.lastName ?: ""}".trim())
                    })
                    put("isSignedIn", true)
                } else {
                    put("user", null)
                    put("isSignedIn", false)
                }
            }
            callbackContext.success(response.toString())
        } catch (e: Exception) {
            Log.e(TAG, "[USER] Exception: ${e.message}", e)
            callbackContext.error("Failed to get user: ${e.message}")
        }
    }

    private fun getSessionToken(callbackContext: CallbackContext) {
        if (!isInitialized) {
            Log.e(TAG, "[TOKEN] Not initialized")
            callbackContext.error("Clerk must be initialized")
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "[TOKEN] Retrieving token...")
                val token = Clerk.auth.getToken()
                
                val response = JSONObject().apply {
                    put("token", token)
                    put("isSignedIn", !token.isNullOrBlank())
                }
                callbackContext.success(response.toString())
            } catch (e: Exception) {
                Log.e(TAG, "[TOKEN] Exception: ${e.message}", e)
                callbackContext.error("Failed to get token: ${e.message}")
            }
        }
    }

    private fun isUserSignedIn(callbackContext: CallbackContext) {
        if (!isInitialized) {
            Log.e(TAG, "[CHECK] Not initialized")
            callbackContext.error("Clerk must be initialized")
            return
        }

        try {
            val isSignedIn = Clerk.user != null
            Log.d(TAG, "[CHECK] Signed in: $isSignedIn")
            
            val response = JSONObject().apply {
                put("isSignedIn", isSignedIn)
            }
            callbackContext.success(response.toString())
        } catch (e: Exception) {
            Log.e(TAG, "[CHECK] Exception: ${e.message}", e)
            callbackContext.error("Failed to check status: ${e.message}")
        }
    }
}