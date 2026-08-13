var exec = require('cordova/exec');

var ClerkPlugin = {
    // Initialize the plugin with your Clerk Publishable Key
    initialize: function (publishableKey, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'initialize', [publishableKey]);
    },
    
    // Manually force reconciliation at specific lifecycle points 
    reloadFromSharedStorage: function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'ClerkPlugin', 'reloadFromSharedStorage', []);
    }
};

module.exports = ClerkPlugin;