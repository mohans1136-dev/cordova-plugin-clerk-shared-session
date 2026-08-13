package com.outsystems.clerk.sharedsession

import org.apache.cordova.CordovaPlugin
import org.apache.cordova.CallbackContext
import org.json.JSONArray
import com.clerk.api.Clerk
import com.clerk.api.ClerkConfigurationOptions
import com.clerk.api.SharedSessionSyncConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ClerkPlugin : CordovaPlugin() {

    private var isInitialized = false

    override fun execute(action: String, args: JSONArray, callbackContext: CallbackContext): Boolean {
        return when (action) {
            "initialize" -> {
                // Read optional JS param if passed, otherwise fallback to strings.xml
                val jsKey = if (args.length() > 0 && !args.isNull(0)) args.optString(0) else null
                initializeClerk(jsKey, callbackContext)
                true
            }
            "reloadFromSharedStorage" -> {
                reloadSharedStorage(callbackContext)
                true
            }
            else -> false
        }
    }

    private fun initializeClerk(jsKey: String?, callbackContext: CallbackContext) {
        if (isInitialized) {
            callbackContext.success("Clerk is already initialized")
            return
        }

        try {
            val context = cordova.activity.applicationContext

            // Retrieve key injected into strings.xml via Cordova plugin variable
            val resId = context.resources.getIdentifier("clerk_publishable_key", "string", context.packageName)
            val resourceKey = if (resId != 0) context.getString(resId) else null

            // Prioritize strings.xml value if it's a valid key (not the placeholder), fallback to JS
            val publishableKey = resourceKey?.takeIf { it.isNotBlank() && !it.startsWith("$") } ?: jsKey

            if (publishableKey.isNullOrBlank()) {
                callbackContext.error("Clerk Publishable Key is missing in Extensibility Configuration and JS parameters.")
                return
            }

            Clerk.initialize(
                context = context,
                publishableKey = publishableKey,
                options = ClerkConfigurationOptions(
                    sharedSessionSync = SharedSessionSyncConfig.enabled
                )
            )
            isInitialized = true
            callbackContext.success("Clerk initialized with Shared Session Sync")
        } catch (e: Exception) {
            callbackContext.error("Failed to initialize Clerk: ${e.message}")
        }
    }

    private fun reloadSharedStorage(callbackContext: CallbackContext) {
        if (!isInitialized) {
            callbackContext.error("Clerk must be initialized before reloading storage")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val stateChanged = Clerk.reloadFromSharedStorage()
                callbackContext.success(if (stateChanged) 1 else 0)
            } catch (e: Exception) {
                callbackContext.error("Error reloading shared storage: ${e.message}")
            }
        }
    }
}