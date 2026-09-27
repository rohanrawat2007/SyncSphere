package com.syncsphere.database;

import com.syncsphere.dao.UserDAO;
import com.syncsphere.model.User;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.PublicMessage;
import com.syncsphere.service.AuthenticationService;
import com.syncsphere.service.ChatService;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DatabasePersistenceIntegrationTest {

    @Test
    void registrationLoginAndMessagesShouldPersistInMysql() throws Exception {
        String username = "dbcheck_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String secondUsername = "dbcheck_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        String password = "StrongPass1!";
        String secondPassword = "StrongPass2!";

        AuthenticationService auth = new AuthenticationService();
        User firstUser = auth.register(username, password);
        User secondUser = auth.register(secondUsername, secondPassword);
        assertNotNull(firstUser);
        assertNotNull(secondUser);

        User loggedIn = auth.login(username, password);
        assertNotNull(loggedIn);
        assertEquals("ONLINE", loggedIn.getStatus());

        UserDAO userDAO = new UserDAO();
        User storedUser = userDAO.findByUsername(username);
        assertNotNull(storedUser);
        assertEquals(username, storedUser.getUsername());

        ChatService chatService = new ChatService();
        PublicMessage publicMessage = chatService.sendPublicMessage(loggedIn.getId(), "Public message persisted to MySQL");
        PrivateMessage privateMessage = chatService.sendPrivateMessage(loggedIn.getId(), secondUser.getId(), "Private message persisted to MySQL");
        assertNotNull(publicMessage);
        assertNotNull(privateMessage);

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement selectUser = connection.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
             PreparedStatement selectPublic = connection.prepareStatement("SELECT COUNT(*) FROM messages WHERE sender_id = ? AND message_type = 'PUBLIC' AND message = ?");
             PreparedStatement selectPrivate = connection.prepareStatement("SELECT COUNT(*) FROM messages WHERE sender_id = ? AND receiver_id = ? AND message_type = 'PRIVATE' AND message = ?")) {
            selectUser.setString(1, username);
            try (ResultSet userResult = selectUser.executeQuery()) {
                assertTrue(userResult.next());
                assertTrue(userResult.getInt(1) >= 1);
            }

            selectPublic.setLong(1, loggedIn.getId());
            selectPublic.setString(2, "Public message persisted to MySQL");
            try (ResultSet publicResult = selectPublic.executeQuery()) {
                assertTrue(publicResult.next());
                assertTrue(publicResult.getInt(1) >= 1);
            }

            selectPrivate.setLong(1, loggedIn.getId());
            selectPrivate.setLong(2, secondUser.getId());
            selectPrivate.setString(3, "Private message persisted to MySQL");
            try (ResultSet privateResult = selectPrivate.executeQuery()) {
                assertTrue(privateResult.next());
                assertTrue(privateResult.getInt(1) >= 1);
            }
        }

        auth.logout(username);
        User postLogout = userDAO.findByUsername(username);
        assertNotNull(postLogout);
        assertEquals("OFFLINE", postLogout.getStatus());
    }
}
