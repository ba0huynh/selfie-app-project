# Quick Fix: 403 access_denied Error

## The Problem

You're seeing this error:
```
403: access_denied
App has not completed Google verification process
```

This happens because your OAuth consent screen is in **"Testing"** mode, and your Google account email is not in the test users list.

## The Solution (5 Steps)

### Step 1: Open Google Cloud Console
Go to: https://console.cloud.google.com/

### Step 2: Select Your Project
- Click the project dropdown at the top
- Select your project (e.g., "Selfie Diary")

### Step 3: Go to OAuth Consent Screen
- Click **"APIs & Services"** in the left menu
- Click **"OAuth consent screen"**

### Step 4: Add Your Email as Test User
- Scroll down to the **"Test users"** section
- Click **"+ ADD USERS"** button
- Enter your **exact Google account email** (the one you're using to sign in)
  - Example: `yourname@gmail.com`
- Click **"ADD"**

### Step 5: Wait and Try Again
- Wait **5-10 minutes** for changes to take effect
- Close and reopen your app
- Try signing in again

## Visual Guide

```
Google Cloud Console
  └─ Your Project
      └─ APIs & Services
          └─ OAuth consent screen
              └─ Scroll down to "Test users"
                  └─ Click "+ ADD USERS"
                      └─ Enter your email
                          └─ Click "ADD"
```

## Common Issues

### "I added my email but still getting 403"
- ✅ Check the email is **exactly** the same (case-sensitive)
- ✅ Make sure you're signing in with the same Google account
- ✅ Wait 10-15 minutes and try again
- ✅ Clear app cache/data: Settings → Apps → Selfie Diary → Storage → Clear Cache

### "I don't see the Test users section"
- Make sure you're on the **"OAuth consent screen"** page
- Scroll down - it's near the bottom of the page
- If you don't see it, your consent screen might not be configured yet - follow the full setup in `OAUTH_SETUP.md`

### "How many test users can I add?"
- You can add up to **100 test users**
- Each user needs to accept the consent screen the first time

## For Production (Later)

When you're ready to publish your app:
1. You'll need to submit your app for Google's verification
2. This process can take several days or weeks
3. Once verified, anyone can sign in (no test users needed)
4. See `OAUTH_SETUP.md` for full production setup

## Still Having Issues?

1. Double-check:
   - ✅ Package name: `com.example.selfie`
   - ✅ SHA-1 fingerprint is correct
   - ✅ Google Drive API is enabled
   - ✅ OAuth client ID is created (Android type)

2. Check the error message in your app - it should now show helpful guidance

3. Review the full setup guide: `OAUTH_SETUP.md`

