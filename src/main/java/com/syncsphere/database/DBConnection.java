package com.syncsphere.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class DBConnection {
    public static final String DEFAULT_HOST = "localhost";
    public static final String DEFAULT_PORT = "3306";
    public static final String DEFAULT_DATABASE = "syncsphere";

    private static final String H2_URL = "jdbc:h2:mem:syncsphere;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";
    private static final String DOT_ENV_FILE = ".env";

    public static String buildJdbcUrl(String host, String port, String database) {
        String effectiveHost = firstNonBlank(host, DEFAULT_HOST);
        String effectivePort = firstNonBlank(port, DEFAULT_PORT);
        String effectiveDatabase = firstNonBlank(database, DEFAULT_DATABASE);

        return "jdbc:mysql://" + effectiveHost + ":" + effectivePort + "/" + effectiveDatabase
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=5000";
    }

    public static String buildJdbcUrl() {
        return buildJdbcUrl(resolveConfigValue("DB_HOST", DEFAULT_HOST),
                resolveConfigValue("DB_PORT", DEFAULT_PORT),
                resolveConfigValue("DB_NAME", DEFAULT_DATABASE));
    }

    public static Connection getConnection() throws SQLException {
        String host = resolveConfigValue("DB_HOST", DEFAULT_HOST);
        String port = resolveConfigValue("DB_PORT", DEFAULT_PORT);
        String database = resolveConfigValue("DB_NAME", DEFAULT_DATABASE);
        String user = resolveConfigValue("DB_USER", "");
        String password = resolveConfigValue("DB_PASSWORD", "");

        if (user == null || user.isBlank()) {
            System.err.println("MySQL is not configured. Set DB_USER and DB_PASSWORD in .env or environment variables.");
            return createFallbackConnection();
        }

        String url = buildJdbcUrl(host, port, database);
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            System.err.println("MySQL connection failed using the local .env or environment configuration. Falling back to the in-memory demo database.");
            return createFallbackConnection();
        }
    }

    public static String resolveConfigValue(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = loadDotEnv().getProperty(key);
        }
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static Properties loadDotEnv() {
        Properties properties = new Properties();
        Path projectRoot = Path.of("").toAbsolutePath();
        Path dotEnvPath = projectRoot.resolve(DOT_ENV_FILE);

        if (!Files.exists(dotEnvPath)) {
            return properties;
        }

        try {
            String content = Files.readString(dotEnvPath, StandardCharsets.UTF_8);
            String[] lines = content.split("\\r?\\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int separator = trimmed.indexOf('=');
                if (separator < 0) {
                    continue;
                }
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                }
                properties.setProperty(key, value);
            }
        } catch (IOException ignored) {
            // Ignore missing or unreadable .env files and rely on process environment vars.
        }

        return properties;
    }

    private static Connection createFallbackConnection() throws SQLException {
        return DriverManager.getConnection(H2_URL, H2_USER, H2_PASSWORD);
    }

    private static String firstNonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
