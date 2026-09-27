# SyncSphere MySQL Setup

This project expects a MySQL database named `syncsphere`.

## 1. Start MySQL

Start your local MySQL server using MySQL Workbench, the MySQL service, or the MySQL CLI.

## 2. Open MySQL Workbench or the MySQL CLI

Examples:

```sql
SHOW DATABASES;
```

## 3. Run the schema

From the MySQL client, run:

```sql
SOURCE C:/projects/SyncSphere/database/schema.sql;
```

Or in MySQL Workbench, open the file and execute it.

## 4. Verify the database exists

```sql
SHOW DATABASES;
USE syncsphere;
SHOW TABLES;
```

You should see the `users` and `messages` tables.

## 5. Configure environment variables

On Windows PowerShell, set:

```powershell
$env:DB_HOST="localhost"
$env:DB_PORT="3306"
$env:DB_NAME="syncsphere"
$env:DB_USER="root"
$env:DB_PASSWORD="YOUR_PASSWORD"
```

Do not print the password. Verify only the non-secret values:

```powershell
echo $env:DB_HOST
echo $env:DB_NAME
echo $env:DB_PORT
echo $env:DB_USER
```

## 6. Start SyncSphere

```powershell
mvn clean compile
mvn exec:java -Dexec.mainClass=com.syncphere.Main
```

## 7. Confirm registration and message persistence

After starting the app:

1. Register a user.
2. Log in.
3. Send messages.
4. Restart the app and reopen the chat.
5. Confirm the messages are still there.

If MySQL is offline or not configured, the application will warn and fall back to the in-memory database so the app still starts without crashing.
