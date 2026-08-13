# Clerk Cordova Plugin - Implementation Guide

**Version:** 2.0.0 (Production-Ready)  
**Last Updated:** August 2026

## Overview

This is a **production-grade Cordova plugin** that provides:
- ✅ Secure hosted authentication via Clerk's Account Portal
- ✅ Multi-app session sharing across sibling Android apps  
- ✅ Comprehensive error handling and logging
- ✅ User and session management
- ✅ Automatic session synchronization

---

## Prerequisites

### Clerk Setup
1. **Enable Native API** in [Clerk Dashboard](https://dashboard.clerk.com/~/native-applications)
2. **Get your Publishable Key** from [API Keys](https://dashboard.clerk.com/~/api-keys)
   - Format: `pk_test_...` (development) or `pk_live_...` (production)
3. **Add your app** to Native applications settings:
   - App namespace
   - Package name

### Android Setup
- **Minimum SDK:** 24+
- **Java version:** 17+
- **Signing certificate:** Must be identical for all participating apps

### Multi-App Setup
All participating apps must:
- Use the **same Clerk Publishable Key**
- Be signed with the **same signing certificate**
- Have **minimum SDK 24+**
- Have **Native API enabled** in Clerk Dashboard

---

## Installation in OutSystems

### Step 1: Add Plugin to Your App

In **OutSystems Studio**:

1. Go to **Module > Extensibility Configurations**
2. Create a new extensibility configuration:
   ```
   Name: CLERK_PUBLISHABLE_KEY
   Value: pk_test_YOUR_KEY_HERE  (or pk_live_...)
   ```

3. Add this plugin via Cordova:
   ```
   cordova plugin add c:\path\to\cordova-plugin-clerk-shared-session
   ```

### Step 2: Initialize Clerk in App Startup

In your **OutSystems Application OnInitialize logic**:

```javascript
// Initialize Clerk SDK
if (cordova && cordova.plugins && cordova.plugins.ClerkPlugin) {
    cordova.plugins.ClerkPlugin.initialize(
        null,  // Will use CLERK_PUBLISHABLE_KEY from extensibility config
        function(success) {
            console.log("✅ Clerk initialized: " + success);
            // Update your app state/variables
            $actions.OnClerkReady();
        },
        function(error) {
            console.error("❌ Clerk initialization failed: " + error);
            $actions.OnClerkFailed(error);
        }
    );
} else {
    console.error("Clerk plugin not available");
}
```

### Step 3: Handle Resume Event for Session Sync

In **OutSystems Application OnResume logic**:

```javascript
// When app returns to foreground, check for session changes from other apps
if (cordova && cordova.plugins && cordova.plugins.ClerkPlugin) {
    cordova.plugins.ClerkPlugin.reloadFromSharedStorage(
        function(result) {
            var response = JSON.parse(result);
            if (response.stateChanged === 1) {
                console.log("📱 Session changed! Reloading user data...");
                // User logged in/out from another app
                $actions.RefreshUserSession();
            }
        },
        function(error) {
            console.error("Failed to reload session: " + error);
        }
    );
}
```

---

## API Reference

### initialize(publishableKey, successCallback, errorCallback)

Initialize the Clerk SDK with shared session sync enabled.

**Parameters:**
- `publishableKey` (string, optional): Your Clerk Publishable Key. If null, uses CLERK_PUBLISHABLE_KEY from config.
- `successCallback` (function): Called on success with initialization message
- `errorCallback` (function): Called on error with error message

**Response:**
```json
{
    "message": "Clerk initialized with Shared Session Sync",
    "initialized": true
}
```

**Example:**
```javascript
cordova.plugins.ClerkPlugin.initialize(
    null,
    function(msg) { console.log(msg); },
    function(err) { console.error(err); }
);
```

---

### startHostedAuth(mode, successCallback, errorCallback)

Start hosted authentication flow (Recommended for production).

Opens Clerk's Account Portal in a secure custom tab browser. Supports both sign-in and sign-up.

**Parameters:**
- `mode` (string): `"signIn"` or `"signUp"`
- `successCallback` (function): Called on successful authentication
- `errorCallback` (function): Called on failure or cancellation

**Response:**
```json
{
    "success": true,
    "message": "Authentication successful",
    "userId": "user_abc123",
    "email": "user@example.com",
    "name": "John Doe"
}
```

**Example:**
```javascript
function loginWithClerk() {
    cordova.plugins.ClerkPlugin.startHostedAuth(
        "signIn",
        function(response) {
            var data = JSON.parse(response);
            console.log("✅ Logged in as: " + data.email);
            $actions.OnAuthSuccess(data);
        },
        function(error) {
            console.error("❌ Login failed: " + error);
            $actions.OnAuthError(error);
        }
    );
}

function signupWithClerk() {
    cordova.plugins.ClerkPlugin.startHostedAuth(
        "signUp",
        function(response) {
            var data = JSON.parse(response);
            console.log("✅ Account created: " + data.userId);
            $actions.OnAuthSuccess(data);
        },
        function(error) {
            console.error("❌ Signup failed: " + error);
        }
    );
}
```

---

### startSignInFlow(successCallback, errorCallback)

Shorthand for `startHostedAuth("signIn", ...)`.

**Equivalent to:**
```javascript
cordova.plugins.ClerkPlugin.startHostedAuth("signIn", successCallback, errorCallback);
```

---

### startSignUpFlow(successCallback, errorCallback)

Shorthand for `startHostedAuth("signUp", ...)`.

**Equivalent to:**
```javascript
cordova.plugins.ClerkPlugin.startHostedAuth("signUp", successCallback, errorCallback);
```

---

### signOut(successCallback, errorCallback)

Sign out the current user and clear the session.

Removes the session from device storage and shared storage. All sibling apps will detect the sign-out on their next foreground or `reloadFromSharedStorage()` call.

**Response:**
```json
{
    "success": true,
    "message": "Signed out successfully"
}
```

**Example:**
```javascript
function logoutUser() {
    cordova.plugins.ClerkPlugin.signOut(
        function(response) {
            console.log("✅ Logged out");
            $actions.ClearUserSession();
        },
        function(error) {
            console.error("❌ Logout failed: " + error);
        }
    );
}
```

---

### reloadFromSharedStorage(successCallback, errorCallback)

Reload session state from shared storage.

Checks if other apps have changed the shared session. Call this:
- When app returns to foreground (`onResume`)
- When user explicitly requests a refresh
- On a timer for automatic updates

**Response:**
```json
{
    "stateChanged": 0 or 1,
    "message": "Session updated" or "No changes"
}
```

**Return values:**
- `stateChanged = 1`: User logged in/out from another app (refresh UI)
- `stateChanged = 0`: No changes detected

**Example:**
```javascript
function checkForSessionChanges() {
    cordova.plugins.ClerkPlugin.reloadFromSharedStorage(
        function(response) {
            var data = JSON.parse(response);
            if (data.stateChanged === 1) {
                console.log("📱 Session changed from another app!");
                $actions.RefreshUI();
            }
        },
        function(error) {
            console.error("❌ Reload failed: " + error);
        }
    );
}
```

---

### getCurrentUser(successCallback, errorCallback)

Get information about the currently signed-in user.

**Response:**
```json
{
    "user": {
        "id": "user_abc123",
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "name": "John Doe"
    },
    "isSignedIn": true
}
```

If not signed in:
```json
{
    "user": null,
    "isSignedIn": false
}
```

**Example:**
```javascript
function getUserInfo() {
    cordova.plugins.ClerkPlugin.getCurrentUser(
        function(response) {
            var data = JSON.parse(response);
            if (data.isSignedIn) {
                console.log("User: " + data.user.name + " (" + data.user.email + ")");
                $actions.DisplayUserInfo(data.user);
            } else {
                console.log("Not signed in");
            }
        },
        function(error) {
            console.error("Failed to get user: " + error);
        }
    );
}
```

---

### getSessionToken(successCallback, errorCallback)

Get the current session token.

Useful for:
- Making authenticated API calls to your backend
- Passing to web components in WebView
- Backend verification

**Response:**
```json
{
    "token": "eyJhbGc...",
    "isSignedIn": true
}
```

If not signed in:
```json
{
    "token": null,
    "isSignedIn": false
}
```

**Example:**
```javascript
function getAuthToken() {
    cordova.plugins.ClerkPlugin.getSessionToken(
        function(response) {
            var data = JSON.parse(response);
            if (data.token) {
                // Use token for authenticated API calls
                fetch('/api/protected', {
                    headers: {
                        'Authorization': 'Bearer ' + data.token
                    }
                });
            }
        },
        function(error) {
            console.error("Failed to get token: " + error);
        }
    );
}
```

---

### isUserSignedIn(successCallback, errorCallback)

Fast check if user is currently signed in (without retrieving full user details).

**Response:**
```json
{
    "isSignedIn": true or false
}
```

**Example:**
```javascript
function checkSignInStatus() {
    cordova.plugins.ClerkPlugin.isUserSignedIn(
        function(response) {
            var data = JSON.parse(response);
            if (data.isSignedIn) {
                $actions.ShowMainUI();
            } else {
                $actions.ShowLoginUI();
            }
        },
        function(error) {
            console.error("Failed to check status: " + error);
        }
    );
}
```

---

## Complete App Lifecycle Example

```javascript
// ============ APPLICATION INITIALIZATION ============
// Call on Application OnInitialize

function AppOnInitialize() {
    if (!cordova) {
        console.error("Cordova not available");
        return;
    }

    // Initialize Clerk
    cordova.plugins.ClerkPlugin.initialize(
        null,
        function() {
            console.log("✅ Clerk initialized");
            // Check if user is already signed in
            checkUserStatus();
        },
        function(error) {
            console.error("❌ Failed to initialize: " + error);
            $actions.ShowError("Could not initialize authentication");
        }
    );
}

function checkUserStatus() {
    cordova.plugins.ClerkPlugin.isUserSignedIn(
        function(response) {
            var data = JSON.parse(response);
            if (data.isSignedIn) {
                $actions.ShowMainScreen();
            } else {
                $actions.ShowLoginScreen();
            }
        },
        function(error) {
            console.error("Error checking status: " + error);
        }
    );
}

// ============ LOGIN BUTTON CLICK ============
function OnLoginButtonClick() {
    cordova.plugins.ClerkPlugin.startHostedAuth(
        "signIn",
        function(response) {
            var data = JSON.parse(response);
            console.log("✅ Login successful: " + data.email);
            $actions.ShowMainScreen();
            $actions.LoadUserData(data);
        },
        function(error) {
            console.error("❌ Login failed: " + error);
            if (error.includes("cancelled")) {
                console.log("User cancelled login");
            } else {
                $actions.ShowError("Login failed: " + error);
            }
        }
    );
}

// ============ LOGOUT BUTTON CLICK ============
function OnLogoutButtonClick() {
    cordova.plugins.ClerkPlugin.signOut(
        function(response) {
            console.log("✅ Logged out");
            $actions.ClearUserData();
            $actions.ShowLoginScreen();
        },
        function(error) {
            console.error("❌ Logout failed: " + error);
            $actions.ShowError("Could not sign out");
        }
    );
}

// ============ APP RESUME (MULTI-APP SYNC) ============
// Call on Application OnResume

function AppOnResume() {
    if (!cordova || !cordova.plugins.ClerkPlugin) {
        return;
    }

    // Check for session changes from other apps
    cordova.plugins.ClerkPlugin.reloadFromSharedStorage(
        function(response) {
            var data = JSON.parse(response);
            if (data.stateChanged === 1) {
                console.log("📱 Detected changes from another app");
                // Refresh user session and UI
                checkUserStatus();
                $actions.RefreshUI();
            }
        },
        function(error) {
            console.log("Could not reload session: " + error);
        }
    );
}

// ============ GET USER PROFILE (OPTIONAL) ============
function LoadUserProfile() {
    cordova.plugins.ClerkPlugin.getCurrentUser(
        function(response) {
            var data = JSON.parse(response);
            if (data.user) {
                var user = data.user;
                console.log("User: " + user.name);
                console.log("Email: " + user.email);
                // Display user info in UI
                $actions.UpdateUserProfile(user);
            }
        },
        function(error) {
            console.error("Failed to load profile: " + error);
        }
    );
}
```

---

## Troubleshooting

### Plugin Not Available
**Error:** `cordova.plugins.ClerkPlugin is undefined`

**Solution:**
- Ensure plugin is installed: `cordova plugin list`
- Rebuild the app: Clean and rebuild in OutSystems
- Check Extensibility Configurations for any errors

### Initialization Fails
**Error:** `Failed to initialize Clerk: ...`

**Likely causes:**
1. Missing or invalid `CLERK_PUBLISHABLE_KEY` in Extensibility Configuration
2. Key doesn't start with `pk_` (must be `pk_test_*` or `pk_live_*`)
3. Native API not enabled in Clerk Dashboard
4. Wrong app package name registered

**Solution:**
- Verify key format in Clerk Dashboard
- Check Extensibility Configuration has correct key
- Rebuild app
- Check Android logcat for detailed errors

### Authentication Opens WebView Instead of Custom Tab
**Problem:** Browser opens inside webview instead of external browser

**Solution:**
- This is expected for hosted auth in Cordova
- Android's Custom Tabs will handle this securely
- No action needed

### Multi-App Sharing Not Working
**Problem:** Session changes in App A aren't detected in App B

**Likely causes:**
1. Apps not signed with same certificate
2. Different Clerk Publishable Keys
3. Not calling `reloadFromSharedStorage()` on app resume
4. Minimum SDK not 24+

**Solution:**
- Verify all apps use same signing certificate
- Verify all apps have identical `CLERK_PUBLISHABLE_KEY`
- Add session reload to `AppOnResume`
- Check minimum SDK in build settings

### Token Retrieval Returns Null
**Problem:** `getSessionToken()` returns null when user is signed in

**Likely cause:**
- Session not fully loaded after authentication

**Solution:**
- Add a 1-2 second delay after `startHostedAuth` before calling `getSessionToken`
- Use `getCurrentUser()` first to ensure session is active

---

## Production Checklist

- [ ] Native API enabled in Clerk Dashboard
- [ ] App registered in Native applications settings
- [ ] Correct `CLERK_PUBLISHABLE_KEY` added to Extensibility Configuration
- [ ] Minimum SDK set to 24+
- [ ] Java version set to 17+
- [ ] `AppOnInitialize` has Clerk initialization
- [ ] `AppOnResume` has `reloadFromSharedStorage()` call
- [ ] Error handling added to all plugin calls
- [ ] Tested login/logout flows
- [ ] Tested multi-app session sharing (if applicable)
- [ ] Tested on actual Android device (minimum SDK 24+)
- [ ] All participating apps signed with same certificate
- [ ] Session timeout behavior tested

---

## Support & Debugging

### Enable Detailed Logging
Check Android logcat for logs starting with `[ClerkPlugin]`:

```bash
adb logcat | grep ClerkPlugin
```

### Common Log Messages
- `[INIT]` - Initialization flow
- `[AUTH]` - Authentication flow
- `[LOGOUT]` - Sign-out flow
- `[RELOAD]` - Session reload
- `[USER]` - User retrieval
- `[TOKEN]` - Token retrieval

### Report Issues
Include in issue reports:
- Exact error message
- Android logcat output
- Steps to reproduce
- App version and SDK version

---

## Version History

**v2.0.0** (Current - Production Ready)
- ✅ Complete rewrite based on official Clerk Android SDK
- ✅ Hosted authentication (Account Portal)
- ✅ Comprehensive error handling
- ✅ Full user and session management
- ✅ Production-grade logging

**v1.0.0** (Deprecated)
- Initial version with basic functionality

---

## License

MIT License - See LICENSE file for details
