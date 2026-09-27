package com.syncsphere.service;

import com.syncsphere.dao.FriendDAO;
import com.syncsphere.model.Friend;
import com.syncsphere.model.FriendRequest;
import com.syncsphere.model.User;

import java.sql.SQLException;
import java.util.List;

public class FriendService {
    private final FriendDAO friendDAO = new FriendDAO();
    private final SessionManager sessions = SessionManager.getInstance();

    public User searchUser(User authenticatedUser, String username) throws SQLException {
        User actor = sessions.requireAuthenticated(authenticatedUser);
        if (username == null || username.isBlank()) return null;
        return friendDAO.findByExactUsername(username.trim(), actor.getId());
    }

    public List<User> searchUsers(User authenticatedUser, String query) throws SQLException {
        User actor = sessions.requireAuthenticated(authenticatedUser);
        return friendDAO.searchUsers(query, actor.getId());
    }

    public long sendRequest(User authenticatedUser, long receiverId) throws SQLException {
        User actor = sessions.requireAuthenticated(authenticatedUser);
        if (actor.getId().equals(receiverId)) throw new IllegalArgumentException("You cannot add yourself.");
        if (receiverId <= 0 || !userExists(receiverId)) throw new IllegalArgumentException("User does not exist.");
        if (friendDAO.areFriends(actor.getId(), receiverId)) throw new IllegalStateException("You are already friends.");
        FriendRequest reverse = friendDAO.findDirectedRequest(receiverId, actor.getId());
        if (reverse != null && "PENDING".equals(reverse.status())) throw new IllegalStateException("This user already requested you.");
        long requestId = friendDAO.createOrReopenRequest(actor.getId(), receiverId);
        friendDAO.addRequestNotification(receiverId, requestId, "FRIEND_REQUEST", actor.getUsername() + " wants to add you as a friend.");
        return requestId;
    }

    public List<FriendRequest> incomingRequests(User authenticatedUser) throws SQLException { return friendDAO.incomingRequests(sessions.requireAuthenticated(authenticatedUser).getId()); }

    public void acceptRequest(User authenticatedUser, long requestId) throws SQLException {
        User actor = sessions.requireAuthenticated(authenticatedUser);
        FriendRequest request = friendDAO.findRequest(requestId);
        if (request == null || !actor.getId().equals(request.receiver().getId())) throw new IllegalStateException("You cannot accept this request.");
        friendDAO.respond(requestId, actor.getId(), "ACCEPTED");
        friendDAO.createFriendship(actor.getId(), request.sender().getId());
        friendDAO.addRequestNotification(request.sender().getId(), requestId, "FRIEND_ACCEPTED", actor.getUsername() + " accepted your friend request.");
    }

    public void declineRequest(User authenticatedUser, long requestId) throws SQLException {
        User actor = sessions.requireAuthenticated(authenticatedUser);
        FriendRequest request = friendDAO.findRequest(requestId);
        if (request == null || !actor.getId().equals(request.receiver().getId())) throw new IllegalStateException("You cannot decline this request.");
        friendDAO.respond(requestId, actor.getId(), "DECLINED");
        friendDAO.addRequestNotification(request.sender().getId(), requestId, "FRIEND_DECLINED", actor.getUsername() + " declined your friend request.");
    }

    public List<Friend> getFriends(User authenticatedUser) throws SQLException { return friendDAO.findFriends(sessions.requireAuthenticated(authenticatedUser).getId()); }

    public void removeFriend(User authenticatedUser, long friendId) throws SQLException {
        User actor = sessions.requireAuthenticated(authenticatedUser);
        if (actor.getId().equals(friendId) || !userExists(friendId) || !friendDAO.areFriends(actor.getId(), friendId)) throw new IllegalStateException("Friendship does not exist.");
        friendDAO.removeFriendship(actor.getId(), friendId);
    }

    private boolean userExists(long userId) throws SQLException {
        return friendDAO.findById(userId) != null;
    }
}
