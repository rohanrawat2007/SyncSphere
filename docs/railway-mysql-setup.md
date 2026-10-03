# Supabase PostgreSQL setup

This project can use a hosted database as its shared database. The Swing clients connect to the same database, so users on different networks see the same accounts, friends, and messages.

Supabase's free tier can host the shared PostgreSQL database for a small SyncSphere deployment. Use [database/schema-postgres.sql](../database/schema-postgres.sql) and set `DB_TYPE=postgres`.

## Create the database

1. Open [supabase.com](https://supabase.com/) and create a free project.
2. Open **Project Settings -> Database**.
3. Copy the direct connection host, port, database name, user, and password.
4. Open **SQL Editor**, paste [database/schema-postgres.sql](../database/schema-postgres.sql), and run it.

Do not commit the generated credentials.

## Configure each client

Create a local `.env` file in the project root on every computer running SyncSphere:

```env
DB_TYPE=postgres
DB_HOST=db.your-project.supabase.co
DB_PORT=5432
DB_NAME=postgres
DB_USER=postgres
DB_PASSWORD=your-supabase-database-password
DB_SSL=true
```

Use the exact values Supabase provides. `DB_SSL=true` makes the JDBC connection require TLS. Every client must use the same Supabase values; otherwise the app falls back to a separate in-memory database and will not share data.

## Run

```powershell
mvn clean test
mvn exec:java
```

The Supabase project must remain active while users connect. For production use, add a backend API rather than exposing database credentials in desktop clients. This setup is appropriate for a controlled prototype or school project where the `.env` file is kept private.
