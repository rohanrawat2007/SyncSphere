# SyncSphere

SyncSphere is a Java Swing chat application that demonstrates a secure multi-threaded chat room, JDBC-backed persistence, and core OOP concepts expected in a college semester project.

## Features

- User registration and login with secure password hashing
- Public messaging with timestamps and user names
- Private messaging between online users
- Online user list with live status updates
- Moderator controls for deleting messages, muting, and unmuting users
- Search by message text or sender
- Persistent reactions, read receipts, pinned messages, and notifications
- Owner-only message deletion/edit authorization and lightweight flood protection
- Resizable three-pane chat workspace with theme toggle and keyboard focus shortcuts
- JDBC persistence with a MySQL schema
- Optional Google OAuth integration
- Native browser OAuth with loopback callback, state validation, and PKCE
- Modern dark glassmorphism-inspired Swing UI

## OOP concepts demonstrated

- Classes and objects
- Encapsulation
- Inheritance
- Abstraction
- Interfaces
- Polymorphism
- Exception handling
- Collections
- Java streams and lambdas
- Multithreading and synchronization
- JDBC and DAO patterns

## Technology stack

- Java 21
- Maven
- Swing
- MySQL Connector/J 8.4.0
- JDBC
- JUnit 5
- Gson 2.11.0

## Architecture

The project is organized into packages such as model, service, dao, thread, filter, exception, gui, database, and util.

## MySQL database setup

The application expects a MySQL database named `syncsphere`.

1. Start MySQL.
2. Open MySQL Workbench or the MySQL CLI.
3. Run `database/schema.sql`.
4. Confirm the database `syncsphere` exists.
5. Configure the environment variables shown in `.env.example`.
6. Start SyncSphere.

## Local configuration

Create a local `.env` from `.env.example`. The application loads it automatically from the project root, so manual exports are not required. Keep the file ignored by Git and never place real values in source code.

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="syncsphere"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_PASSWORD"
```

### Supabase free-tier configuration

For two computers on different networks, use the linked Supabase PostgreSQL project as the shared database. Run `database/schema-postgres.sql` in the Supabase SQL Editor, then create the same `.env` values on both computers:

```env
DB_TYPE=postgres
DB_HOST=db.bdmaqszzouirvldgdjyd.supabase.co
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=YOUR_SUPABASE_DATABASE_PASSWORD
DB_SSL=true
```

Keep `.env` private. If PostgreSQL mode is configured incorrectly, SyncSphere stops with a clear connection error instead of falling back to isolated local data.

### Credential-free desktop distribution

For the packaged desktop application, set only `SYNCSPHERE_API_URL` to the deployed HTTPS website/API URL. Do not distribute `DB_PASSWORD`, `SUPABASE_SERVICE_ROLE_KEY`, or any direct database URL with the executable. Configure `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, and `SYNCSPHERE_SESSION_SECRET` only on the API server.

When the desktop app starts without an API URL, it opens **SyncSphere connection setup**. Paste the public API URL and choose **Save API**. The URL is stored at `%USERPROFILE%\\.syncsphere\\config.properties`; choose **Use local mode** only for local development. Install the same executable on the second computer and enter the same API URL there.

For the website/API deployment, configure these server-only variables in the hosting provider:

```env
SUPABASE_URL=https://bdmaqszzouirvldgdjyd.supabase.co
SUPABASE_SERVICE_ROLE_KEY=YOUR_SERVER_ONLY_KEY
SYNCSPHERE_SESSION_SECRET=YOUR_RANDOM_LONG_SECRET
```

Google OAuth uses the Desktop client variables `GOOGLE_OAUTH_ENABLED`, `GOOGLE_CLIENT_ID`, and `GOOGLE_CLIENT_SECRET`. The web client variables are reserved for the separate website and are not used by this Swing application.

## How to run

```powershell
mvn clean compile
mvn exec:java -Dexec.mainClass=com.syncsphere.Main
```

If MySQL is unavailable or is not configured yet, the app falls back to the in-memory demo database for local UI exploration.

## Database verification after setup

```sql
SHOW DATABASES;
USE syncsphere;
SHOW TABLES;
SELECT * FROM users;
SELECT * FROM messages;
SELECT * FROM message_reactions;
SELECT * FROM message_reads;
SELECT * FROM pinned_messages;
SELECT * FROM notifications;
```

## Testing

```powershell
mvn clean test
```

The suite covers database connectivity, registration/login, public/private persistence, and reaction/read/pin persistence.

## Screenshots

Add screenshots here once the project is running locally.

## Team member

- Developer: SyncSphere Team

## Future scope

- SMTP notifications
- Better moderation analytics
- Improved audit logs
- Web version
