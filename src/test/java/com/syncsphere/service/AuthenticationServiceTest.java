package com.syncsphere.service;

import com.syncsphere.exception.InvalidLoginException;
import com.syncsphere.exception.UserAlreadyExistsException;
import com.syncsphere.model.User;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {

    private String uniqueUsername(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    @Test
    void registerAndLoginShouldWork() throws Exception {
        AuthenticationService auth = new AuthenticationService();
        String username = uniqueUsername("alice");
        User user = auth.register(username, "StrongPass1!");

        assertNotNull(user);
        assertEquals(username, user.getUsername());
        assertEquals("USER", user.getRole());

        User loggedIn = auth.login(username, "StrongPass1!");
        assertNotNull(loggedIn);
        assertTrue(loggedIn.isOnline());
    }

    @Test
    void duplicateUsernameShouldFail() throws Exception {
        AuthenticationService auth = new AuthenticationService();
        String username = uniqueUsername("bob");
        auth.register(username, "StrongPass2!");

        UserAlreadyExistsException ex = assertThrows(UserAlreadyExistsException.class,
                () -> auth.register(username, "AnotherPass3!"));

        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void invalidLoginShouldFail() throws Exception {
        AuthenticationService auth = new AuthenticationService();
        String username = uniqueUsername("charlie");
        auth.register(username, "StrongPass4!");

        assertThrows(InvalidLoginException.class,
                () -> auth.login(username, "wrongPassword"));
    }
}
