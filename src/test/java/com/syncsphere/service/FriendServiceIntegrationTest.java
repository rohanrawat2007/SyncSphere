package com.syncsphere.service;

import com.syncsphere.database.DBConnection;
import com.syncsphere.model.FriendRequest;
import com.syncsphere.model.User;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendServiceIntegrationTest {
    @Test
    void realUsersCanSearchRequestAcceptChatAndRemove() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        AuthenticationService authentication = new AuthenticationService();
        User first = authentication.login(authentication.register("fa_" + suffix, "StrongPass1! ").getUsername(), "StrongPass1! ");
        User second = authentication.register("fb_" + suffix, "StrongPass2!");
        FriendService friends = new FriendService();

        assertEquals(second.getId(), friends.searchUser(first, second.getUsername()).getId());
        assertThrows(IllegalArgumentException.class, () -> friends.sendRequest(first, first.getId()));
        long requestId = friends.sendRequest(first, second.getId());
        assertThrows(IllegalStateException.class, () -> friends.sendRequest(first, second.getId()));

        authentication.login(second.getUsername(), "StrongPass2!");
        FriendRequest incoming = friends.incomingRequests(second).stream().filter(request -> request.id().equals(requestId)).findFirst().orElse(null);
        assertNotNull(incoming);
        friends.acceptRequest(second, requestId);
        assertTrue(friends.getFriends(first).stream().anyMatch(friend -> friend.user().getId().equals(second.getId())));
        assertTrue(friends.getFriends(second).stream().anyMatch(friend -> friend.user().getId().equals(first.getId())));

        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM friends WHERE user_id = ? AND friend_id = ?")) {
            statement.setLong(1, Math.min(first.getId(), second.getId())); statement.setLong(2, Math.max(first.getId(), second.getId()));
            try (ResultSet result = statement.executeQuery()) { assertTrue(result.next()); assertEquals(1, result.getInt(1)); }
        }

        FriendRequest declinedRequest = null;
        friends.removeFriend(second, first.getId());
        assertFalse(friends.getFriends(second).stream().anyMatch(friend -> friend.user().getId().equals(first.getId())));
        long secondRequest = friends.sendRequest(second, first.getId());
        authentication.login(first.getUsername(), "StrongPass1! ");
        declinedRequest = friends.incomingRequests(first).stream().filter(request -> request.id().equals(secondRequest)).findFirst().orElseThrow();
        friends.declineRequest(first, declinedRequest.id());
        assertFalse(friends.getFriends(first).stream().anyMatch(friend -> friend.user().getId().equals(second.getId())));
    }
}
