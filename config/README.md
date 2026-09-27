# Configuration Guide

## 1. MySQL setup

1. Install MySQL and create a database named `syncsphere`.
2. Import the schema from `database/schema.sql`.
3. Create a local `.env` file from `.env.example`.
4. Fill in the values for:
   - `DB_HOST`
   - `DB_PORT`
   - `DB_NAME`
   - `DB_USER`
   - `DB_PASSWORD`

Do not commit the `.env` file or any real credentials to version control.

## 2. Optional Google OAuth setup

1. Create a Google Cloud project.
2. Configure the OAuth consent screen.
3. Create OAuth 2.0 credentials.
4. Set the redirect URI to `http://localhost:8080/oauth2/callback`.
5. Add the resulting values to your `.env` file:
   - `GOOGLE_CLIENT_ID`
   - `GOOGLE_CLIENT_SECRET`
   - `GOOGLE_REDIRECT_URI`

If Google credentials are not configured, the app continues with local username/password login.

## 3. Run the application

```bash
mvn clean compile
mvn exec:java
```
