var exec = require('cordova/exec');

/**
 * Clerk Cordova Plugin - Production-Ready Authentication Bridge
 * 
 * Provides seamless integration with Clerk's Native Android SDK for:
 * - Secure hosted authentication (Account Portal)
 * - Multi-app session sharing
 * - User and session management
 * - Automatic session synchronization across sibling apps
 */
var ClerkPlugin = {
    
    /**
     * Initialize Clerk SDK with shared session sync enabled
     * 
     * Must be called before any other operations.
     * Loads publishable key from:
     * 1. JavaScript parameter (if provided)
     * 2. CLERK_PUBLISHABLE_KEY from plugin configuration
     * 
     * @param {string} publishableKey - Optional Clerk Publishable Key (pk_*)
     * @param {function} successCallback - Called on successful initialization
     * @param {function} errorCallback - Called if initialization fails
     */
    initialize: function (publishableKey, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'initialize', [publishableKey]);
    },
    
    /**
     * Start hosted authentication flow (Recommended)
     * 
     * Opens Clerk's Account Portal in a secure custom tab browser.
     * Automatically handles both sign-in and sign-up flows.
     * 
     * @param {string} mode - 'signIn' (default) or 'signUp'
     * @param {function} successCallback - Called on successful authentication
     * @param {function} errorCallback - Called if authentication fails or is cancelled
     */
    startHostedAuth: function (mode, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'startHostedAuth', [mode]);
    },
    
    /**
     * Start sign-in flow via hosted authentication
     * 
     * Alias for startHostedAuth('signIn')
     * 
     * @param {function} successCallback - Called on successful sign-in
     * @param {function} errorCallback - Called if sign-in fails
     */
    startSignInFlow: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'startSignInFlow', []);
    },
    
    /**
     * Start sign-up flow via hosted authentication
     * 
     * Alias for startHostedAuth('signUp')
     * 
     * @param {function} successCallback - Called on successful sign-up
     * @param {function} errorCallback - Called if sign-up fails
     */
    startSignUpFlow: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'startSignUpFlow', []);
    },
    
    /**
     * Sign out the current user
     * 
     * Clears the session from the device and shared storage.
     * All sibling apps will detect the sign-out on their next
     * foreground event or reloadFromSharedStorage() call.
     * 
     * @param {function} successCallback - Called on successful sign-out
     * @param {function} errorCallback - Called if sign-out fails
     */
    signOut: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'signOut', []);
    },
    
    /**
     * Reload session from shared storage
     * 
     * Checks if other apps have changed the shared session state.
     * Call this:
     * - When app returns to foreground (in onResume)
     * - When user explicitly requests a refresh
     * - On a timer for automatic updates
     * 
     * Returns: { stateChanged: 0|1, message: string }
     * - stateChanged = 1: User logged in/out from another app (refresh UI)
     * - stateChanged = 0: No changes detected
     * 
     * @param {function} successCallback - Called with reload result
     * @param {function} errorCallback - Called if reload fails
     */
    reloadFromSharedStorage: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'reloadFromSharedStorage', []);
    },
    
    /**
     * Get current authenticated user
     * 
     * Returns: { user: {...}, isSignedIn: boolean }
     * 
     * User object contains:
     * - id: User's unique identifier
     * - email: Primary email address
     * - firstName: First name
     * - lastName: Last name
     * - name: Full name (concatenated)
     * 
     * @param {function} successCallback - Called with user data
     * @param {function} errorCallback - Called if retrieval fails
     */
    getCurrentUser: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'getCurrentUser', []);
    },
    
    /**
     * Get current session token
     * 
     * Useful for:
     * - Making authenticated API calls to your backend
     * - Passing to web components in WebView
     * - Backend verification of user session
     * 
     * Returns: { token: string|null, isSignedIn: boolean }
     * 
     * @param {function} successCallback - Called with session token
     * @param {function} errorCallback - Called if retrieval fails
     */
    getSessionToken: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'getSessionToken', []);
    },
    
    /**
     * Check if user is currently signed in
     * 
     * Fast check without retrieving full user details.
     * 
     * Returns: { isSignedIn: boolean }
     * 
     * @param {function} successCallback - Called with sign-in status
     * @param {function} errorCallback - Called if check fails
     */
    isUserSignedIn: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'isUserSignedIn', []);
    }
};

module.exports = ClerkPlugin;