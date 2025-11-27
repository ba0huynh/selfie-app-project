#!/bin/bash
# Bash script to get SHA-1 fingerprint for Android OAuth setup
# Run this script from the project root directory

echo "Getting SHA-1 fingerprint for Android OAuth setup..."
echo ""

# Default debug keystore path
DEBUG_KEYSTORE="$HOME/.android/debug.keystore"

if [ -f "$DEBUG_KEYSTORE" ]; then
    echo "Found debug keystore at: $DEBUG_KEYSTORE"
    echo ""
    echo "SHA-1 Fingerprint:"
    echo "=================="
    
    # Get SHA-1
    keytool -list -v -keystore "$DEBUG_KEYSTORE" -alias androiddebugkey -storepass android -keypass android | grep -A 1 "SHA1:" | head -2
    
    echo ""
    echo "Copy the SHA-1 value above to Google Cloud Console when creating OAuth credentials."
    echo ""
else
    echo "Debug keystore not found at: $DEBUG_KEYSTORE"
    echo ""
    echo "Please run this command manually:"
    echo "keytool -list -v -keystore \"$DEBUG_KEYSTORE\" -alias androiddebugkey -storepass android -keypass android"
fi


