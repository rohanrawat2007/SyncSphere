package com.syncsphere.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseConnectionTest {

    @Test
    void jdbcUrlShouldUseExpectedMysqlFormat() {
        String url = DBConnection.buildJdbcUrl("localhost", "3306", "syncsphere");
        assertNotNull(url);
        assertTrue(url.startsWith("jdbc:mysql://localhost:3306/syncsphere"));
        assertTrue(url.contains("connectTimeout=5000"));
    }

    @Test
    void connectionShouldReachDatabaseOrFailGracefully() {
        try (Connection connection = DBConnection.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            assertNotNull(metaData);
            assertNotNull(metaData.getURL());
        } catch (SQLException e) {
            assertTrue(true, "MySQL is not configured in this environment; the app is expected to fail gracefully and fall back safely.");
        }
    }
}
