# SyncSphere Cross-Device Chat Roadmap

## Goal

Allow the same SyncSphere account and conversations to work from different computers and networks without shipping Supabase database credentials inside the desktop application.

## Target architecture

```text
SyncSphere.exe -> HTTPS API -> Supabase PostgreSQL
```

The desktop app contains only the public API URL. Supabase credentials and database access remain server-side.

## Completed

- Supabase PostgreSQL project created and restored.
- SyncSphere PostgreSQL schema applied to Supabase.
- JDBC PostgreSQL support added for local validation.
- Supabase schema verified with live CLI queries.
- Server-side Next.js API route created at `/api/syncsphere`.
- API supports registration, login, signed sessions, logout, user search, friend requests, public/private message reads, and sending messages.
- Next.js production build passes.

## Remaining implementation

### 1. Deploy the API

- Deploy `syncsphere-website` to a server platform.
- Configure server-only variables:
  - `SUPABASE_URL`
  - `SUPABASE_SERVICE_ROLE_KEY`
  - `SYNCSPHERE_SESSION_SECRET`
- Verify the HTTPS API health and authentication endpoints.

### 2. Add the Java API client

- Add a Java `RemoteApiClient` using `java.net.http.HttpClient`.
- Store only `SYNCSPHERE_API_URL` in the desktop app configuration.
- Keep session tokens in memory and send them as `Authorization: Bearer ...`.
- Do not add Supabase database passwords, service-role keys, or direct database URLs to the desktop client.

### 3. Migrate desktop services

Replace direct DAO calls with API calls for:

- Authentication and logout
- Public and private messages
- User presence
- Friend search, requests, acceptance, decline, and removal
- Notifications
- Reactions, read receipts, and pins
- Moderation actions

The Swing UI and service contracts should remain stable while the persistence boundary changes.

### 4. Improve real-time behavior

- Keep polling as a reliable fallback.
- Add Supabase Realtime or server-sent events for new messages and presence.
- Reconnect automatically after network loss.
- Show a clear offline/API-unavailable state.

### 5. Package the desktop client

- Build the application with Maven.
- Use `jpackage` to create a Windows `.exe` installer.
- Bundle a public API URL configuration file, never database credentials.
- Test installation on a clean Windows computer.

### 6. Validate cross-device behavior

- Register on computer A.
- Log in on computer B.
- Send public and private messages in both directions.
- Test friend search and requests across devices.
- Disconnect and reconnect one client.
- Confirm no database password is present in the installer or executable strings.

## Definition of done

- Two computers on different networks can log in to the same deployed API.
- Messages, friends, notifications, and presence are shared.
- The executable contains no Supabase database password or service-role key.
- API secrets exist only in server-side deployment variables.
- Clean-install and Maven/Next.js verification both pass.
