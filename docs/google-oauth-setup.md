# Google OAuth Setup

This project supports Google sign-in as an optional authentication method. The app does not store Google passwords or client secrets in source code.

## 1. Create a Google Cloud project

1. Open the Google Cloud Console.
2. Create a new project or choose an existing one.
3. Enable the Google Identity / OAuth APIs required for your project.

## 2. Configure the OAuth consent screen

1. Navigate to APIs & Services > OAuth consent screen.
2. Choose the user type.
3. Add the required app information.
4. Save the consent screen configuration.

## 3. Create OAuth credentials

1. Go to APIs & Services > Credentials.
2. Click Create Credentials > OAuth 2.0 Client ID.
3. Select Desktop app or Web application depending on your setup.
4. For the Desktop application client, no fixed redirect URI is required. SyncSphere opens a random loopback port and validates the callback with state and PKCE.
5. Save the generated values.

## 4. Store secrets safely

Put the generated values in your local `.env` file, not in source code:

```dotenv
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_OAUTH_ENABLED=true
```

## 5. Test the callback

Start the app and choose the Google login button. If the environment variables are missing or invalid, the app shows a friendly message instead of crashing.

## 6. Rotation and revocation

If a secret is exposed, revoke it in Google Cloud and create a new OAuth credential immediately.
