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
                val publishableKey = args.getString(0)
                initializeClerk(publishableKey, callbackContext)
                true
            }
            "reloadFromSharedStorage" -> {
                reloadSharedStorage(callbackContext)
                true
            }
            else -> false
        }
    }

    private fun initializeClerk(publishableKey: String, callbackContext: CallbackContext) {
        if (isInitialized) {
            callbackContext.success("Clerk is already initialized")
            return
        }
        
        try {
            val context = cordova.activity.applicationContext
            
            // Pass SharedSessionSyncConfig.enabled to ClerkConfigurationOptions
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
        
        // Execute manual reconciliation from a coroutine 
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val stateChanged = Clerk.reloadFromSharedStorage()
                // Returns 1 if state changed (true), 0 if false
                callbackContext.success(if (stateChanged) 1 else 0) 
            } catch (e: Exception) {
                callbackContext.error("Error reloading shared storage: ${e.message}")
            }
        }
    }
}