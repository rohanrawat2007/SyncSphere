# Railway MySQL setup

This project can use a Railway MySQL service as its shared database. The Swing clients connect to the same database, so users on different networks see the same accounts, friends, and messages.

## Create the database

1. Open [railway.app](https://railway.app/) and create a project.
2. Add a MySQL service.
3. Open the MySQL service's **Variables** tab.
4. Copy the host, port, database name, username, and password values.
5. Run `database/schema.sql` against that database using MySQL Workbench or the Railway CLI.

Do not commit the generated credentials.

## Configure each client

Create a local `.env` file in the project root on every computer running SyncSphere:

```env
DB_HOST=your-railway-host
DB_PORT=3306
DB_NAME=railway
DB_USER=your-railway-user
DB_PASSWORD=your-railway-password
DB_SSL=true
```

Use the exact values Railway provides. `DB_SSL=true` makes the JDBC connection require TLS. Every client must use the same Railway values; otherwise the app falls back to a separate in-memory database and will not share data.

## Run

```powershell
mvn clean test
mvn exec:java
```

The Railway database must remain running while users connect. For production use, add a backend API rather than exposing database credentials in desktop clients. This setup is appropriate for a controlled prototype or school project where the `.env` file is kept private.
