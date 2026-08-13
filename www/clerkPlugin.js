var exec = require('cordova/exec');

var ClerkPlugin = {
    // Initialize the plugin with your Clerk Publishable Key
    initialize: function (publishableKey, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'initialize', [publishableKey]);
    },
    
    // Native sign-in via Clerk's Custom Tabs browser
    // This bridges WebView auth with Native SDK for shared session sync
    startSignInFlow: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'startSignInFlow', []);
    },
    
    // Native sign-up via Clerk's Custom Tabs browser
    startSignUpFlow: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'startSignUpFlow', []);
    },
    
    // Native sign-out (clears session from native SDK and shared storage)
    signOut: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'signOut', []);
    },
    
    // Manually force reconciliation at specific lifecycle points 
    reloadFromSharedStorage: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'reloadFromSharedStorage', []);
    }
};

module.exports = ClerkPlugin;