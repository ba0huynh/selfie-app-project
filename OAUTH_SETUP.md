# Google Drive OAuth Setup Guide

This guide will help you set up OAuth 2.0 for Google Drive integration in your Android app.

## Prerequisites

- Google account
- Android Studio installed
- Your app package name: `com.example.selfie`

## Step 1: Get Your SHA-1 Fingerprint

You need the SHA-1 fingerprint of your debug keystore to create OAuth credentials.

### Option A: Using Gradle (Recommended)

Run this command in your project root:

**Windows:**
```powershell
cd android
.\gradlew signingReport
```

**Mac/Linux:**
```bash
cd android
./gradlew signingReport
```

Look for the SHA-1 value under `Variant: debug` → `SHA1:` in the output.

### Option B: Using Keytool Command

**Windows:**
```powershell
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Mac/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

Copy the SHA-1 value (it looks like: `AA:BB:CC:DD:EE:FF:...`)

## Step 2: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click on the project dropdown at the top
3. Click **"New Project"**
4. Enter project name (e.g., "Selfie Diary")
5. Click **"Create"**
6. Wait for the project to be created and select it

## Step 3: Enable Google Drive API

1. In your Google Cloud project, go to **"APIs & Services"** → **"Library"**
2. Search for **"Google Drive API"**
3. Click on it and click **"Enable"**
4. Wait for it to be enabled

## Step 4: Create OAuth 2.0 Credentials

1. Go to **"APIs & Services"** → **"Credentials"**
2. Click **"+ CREATE CREDENTIALS"** at the top
3. Select **"OAuth client ID"**
4. If prompted, configure the OAuth consent screen first:
   - Choose **"External"** (unless you have a Google Workspace account)
   - Fill in:
     - App name: "Selfie Diary"
     - User support email: Your email
     - Developer contact: Your email
   - Click **"Save and Continue"**
   - Add scopes: Click **"Add or Remove Scopes"** → Search for `drive.file` → Select it → **"Update"** → **"Save and Continue"**
   - **IMPORTANT:** Add test users:
     - Click **"+ ADD USERS"**
     - Enter your Google account email (the one you'll use to sign in)
     - Click **"ADD"**
     - You can add multiple emails (one per line)
   - Click **"Save and Continue"**
   - Review and **"Back to Dashboard"**

   **Note:** If you already configured the consent screen, you can add test users later by going to **"APIs & Services"** → **"OAuth consent screen"** → Scroll to **"Test users"** section → **"+ ADD USERS"**

5. Now create the OAuth client ID:
   - Application type: Select **"Android"**
   - Name: "Selfie Diary Android"
   - Package name: `com.example.selfie`
   - SHA-1 certificate fingerprint: Paste the SHA-1 you got from Step 1
   - Click **"Create"**

6. **Important:** Copy the **Client ID** (it looks like: `123456789-abcdefghijklmnop.apps.googleusercontent.com`)

## Step 5: Get SHA-1 for Release Build (Optional but Recommended)

For production, you'll also need the SHA-1 of your release keystore:

```bash
keytool -list -v -keystore path/to/your/release.keystore -alias your-key-alias
```

Add this SHA-1 as another Android OAuth client in Google Cloud Console.

## Step 6: Test the Integration

1. Build and run your app
2. Go to Settings → Google Drive
3. Click "Kết nối với Google Drive"
4. You should see the Google Sign-In screen
5. Sign in with your Google account
6. Grant permissions for Google Drive access

## Troubleshooting

### Error: "403 - access_denied" or "App has not completed Google verification process"

**This is the most common error during development!**

**Solution: Add Test Users**

Your OAuth consent screen is in "Testing" mode, which means only users you explicitly add can sign in. Here's how to fix it:

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select your project
3. Go to **"APIs & Services"** → **"OAuth consent screen"**
4. Scroll down to the **"Test users"** section
5. Click **"+ ADD USERS"**
6. Enter the **exact Google account email** you want to use for testing
   - Example: `yourname@gmail.com`
   - You can add multiple test users (one per line)
7. Click **"ADD"**
8. **Wait 5-10 minutes** for the changes to propagate
9. Try signing in again in your app

**Important Notes:**
- You must use the **exact email address** that you're signing in with
- Changes can take a few minutes to take effect
- Each test user must accept the consent screen the first time they sign in
- You can add up to 100 test users

**If you still get 403 after adding test users:**
- Double-check the email address is correct (case-sensitive)
- Make sure you're signing in with the same Google account
- Wait a few more minutes and try again
- Clear the app's cache/data and try again
- Check that the OAuth consent screen is in "Testing" mode (not "In production")

### Error: "10 - Developer Error"
- Make sure the package name matches exactly: `com.example.selfie`
- Verify the SHA-1 fingerprint is correct
- Make sure Google Drive API is enabled
- Check that OAuth client ID is created for Android type

### Error: "12501 - Sign in cancelled"
- User cancelled the sign-in (this is normal)

### Error: "7 - Network Error"
- Check your internet connection
- Make sure the device/emulator has internet access

### App not showing in OAuth consent screen
- Make sure you added your email as a test user
- For production, you need to publish the app and go through verification

## Important Notes

- **Debug vs Release:** You need separate OAuth clients for debug and release builds (different SHA-1)
- **Test Users (CRITICAL):** 
  - During development, your app is in "Testing" mode
  - **Only test users can sign in** - you MUST add your email to the test users list
  - Go to: Google Cloud Console → APIs & Services → OAuth consent screen → Test users
  - Add the exact email address you'll use to sign in
  - Changes may take 5-10 minutes to take effect
  - You can add up to 100 test users
- **403 Error Fix:** If you get "403: access_denied", it means your email is not in the test users list. Add it following the steps above.
- **Production:** Before publishing, you'll need to:
  - Complete OAuth consent screen verification (this is a separate process)
  - Add release SHA-1 fingerprint
  - Submit your app for Google's verification (can take several days/weeks)
  - Publish the app for public use

## Security Best Practices

1. Never commit your release keystore to version control
2. Keep your OAuth client IDs secure
3. Use different OAuth clients for debug and release
4. Regularly rotate credentials if compromised

## Need Help?

- [Google Sign-In Documentation](https://developers.google.com/identity/sign-in/android/start)
- [Google Drive API Documentation](https://developers.google.com/drive/api)
- [OAuth 2.0 for Mobile Apps](https://developers.google.com/identity/protocols/oauth2/native-app)


