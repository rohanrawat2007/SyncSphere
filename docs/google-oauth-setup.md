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

## 4. Vercel & Multi-Device Setup (Fixing `redirect_uri_mismatch`)

When deploying the API to Vercel, Google OAuth requires registering the exact callback URL of your Vercel deployment:

1. Open Google Cloud Console -> **APIs & Services** -> **Credentials**.
2. Select your **OAuth 2.0 Client ID** (Web application type).
3. Under **Authorized JavaScript origins**, add:
   - `https://<your-vercel-domain>.vercel.app` (e.g. `https://syncsphere-website.vercel.app`)
4. Under **Authorized redirect URIs**, add:
   - `https://<your-vercel-domain>.vercel.app/api/syncsphere/google/callback`
   - `http://localhost:8080/oauth2/callback` (for local Java desktop direct mode)
   - `http://localhost:3000/api/syncsphere/google/callback` (for local website dev)
5. Save the credentials.

## 5. Store secrets in Vercel & local `.env`

In Vercel project environment variables, add:
- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- `SYNCSPHERE_SESSION_SECRET`
- `SUPABASE_URL`
- `SUPABASE_SERVICE_ROLE_KEY`

In your local `.env` file for desktop/local development:

```dotenv
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret
GOOGLE_OAUTH_ENABLED=true
SYNCSPHERE_API_URL=https://<your-vercel-domain>.vercel.app
```

## 6. Test the callback & multi-device login

Start the desktop app or connect via Vercel HTTPS API. Choose Google Sign-In:
1. Desktop opens browser to Vercel `/api/syncsphere/google/start`.
2. Vercel redirects to Google Auth with `redirect_uri=https://<your-domain>/api/syncsphere/google/callback`.
3. User signs in with Google.
4. Google redirects back to Vercel callback, which validates identity and returns session token to desktop app via loopback.

