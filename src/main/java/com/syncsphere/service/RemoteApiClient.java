package com.syncsphere.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.syncsphere.database.DBConnection;
import com.syncsphere.model.User;
import com.syncsphere.model.Message;
import com.syncsphere.model.PrivateMessage;
import com.syncsphere.model.PublicMessage;
import com.syncsphere.model.Friend;
import com.syncsphere.model.FriendRequest;
import com.syncsphere.model.Notification;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.awt.Desktop;
import java.net.InetAddress;
import java.net.URI;
import java.net.ServerSocket;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Client for the credential-free HTTPS API used by distributed desktop clients. */
public final class RemoteApiClient {
    private static final String API_URL_KEY = "SYNCSPHERE_API_URL";
    private static final String DEFAULT_API_URL = "https://syncsphere-website.vercel.app";
    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final Gson GSON = new Gson();
    private static volatile String sessionToken;

    private RemoteApiClient() { }

    public static boolean isConfigured() {
        String url = apiUrl();
        return !url.isBlank() && !url.contains("your-api-host");
    }

    public static String apiUrl() {
        String configured = DBConnection.resolveConfigValue(API_URL_KEY, "");
        if (!configured.isBlank()) return configured;
        String property = System.getProperty(API_URL_KEY);
        if (property != null && !property.isBlank()) return property.trim();
        Path config = Path.of(System.getProperty("user.home"), ".syncsphere", "config.properties");
        if (Files.exists(config)) {
            try {
                Properties properties = new Properties();
                try (var reader = Files.newBufferedReader(config)) { properties.load(reader); }
                return properties.getProperty(API_URL_KEY, "").trim();
            } catch (IOException ignored) { }
        }
        return DEFAULT_API_URL;
    }

    public static void saveApiUrl(String url) throws IOException {
        String normalized = url == null ? "" : url.trim().replaceAll("/$", "");
        if (!normalized.isBlank() && !normalized.startsWith("https://") && !normalized.startsWith("http://localhost")) {
            throw new IllegalArgumentException("Use an HTTPS API URL.");
        }
        Path directory = Path.of(System.getProperty("user.home"), ".syncsphere");
        Files.createDirectories(directory);
        Properties properties = new Properties();
        properties.setProperty(API_URL_KEY, normalized);
        try (var writer = Files.newBufferedWriter(directory.resolve("config.properties"))) { properties.store(writer, "SyncSphere desktop configuration"); }
        System.setProperty(API_URL_KEY, normalized);
    }

    public static User register(String username, String password) throws IOException, InterruptedException {
        JsonObject response = post(new JsonObjectBuilder().put("action", "register").put("username", username).put("password", password).build(), null);
        return acceptSession(response);
    }

    public static User login(String username, String password) throws IOException, InterruptedException {
        JsonObject response = post(new JsonObjectBuilder().put("action", "login").put("username", username).put("password", password).build(), null);
        return acceptSession(response);
    }

    public static void logout() throws IOException, InterruptedException {
        if (!isConfigured() || sessionToken == null) return;
        try { post(new JsonObjectBuilder().put("action", "logout").build(), sessionToken); }
        finally { sessionToken = null; }
    }

    public static String sessionToken() { return sessionToken; }

    public static User googleLogin() throws IOException {
        if (!isConfigured()) throw new IOException("Remote API is not configured.");
        try (ServerSocket callbackServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            callbackServer.setSoTimeout(180_000);
            String callback = "http://127.0.0.1:" + callbackServer.getLocalPort() + "/oauth2/callback";
            String start = apiUrl() + "/api/syncsphere/google/start?redirect_uri=" + java.net.URLEncoder.encode(callback, StandardCharsets.UTF_8);
            if (!Desktop.isDesktopSupported()) throw new IOException("A desktop browser is required for Google sign-in.");
            try { Desktop.getDesktop().browse(URI.create(start)); }
            catch (Exception exception) { throw new IOException("Could not open the Google sign-in page.", exception); }
            try (var socket = callbackServer.accept();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                String requestLine = reader.readLine();
                if (requestLine == null || !requestLine.startsWith("GET ")) throw new IOException("Google callback was invalid.");
                URI callbackUri = URI.create(requestLine.substring(4, requestLine.indexOf(" HTTP/")));
                Map<String, String> values = queryValues(callbackUri.getRawQuery());
                String token = values.get("token");
                String encodedUser = values.get("user");
                String response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\nConnection: close\r\n\r\n<h2>SyncSphere sign-in complete</h2><p>You can return to the app.</p>";
                try (OutputStream output = socket.getOutputStream()) { output.write(response.getBytes(StandardCharsets.UTF_8)); }
                if (token == null || encodedUser == null) throw new IOException("Google sign-in was not completed.");
                sessionToken = token;
                return parseUser(JsonParser.parseString(new String(Base64.getUrlDecoder().decode(encodedUser), StandardCharsets.UTF_8)).getAsJsonObject());
            }
        } catch (java.net.SocketTimeoutException exception) {
            throw new IOException("Google sign-in timed out.", exception);
        }
    }

    private static Map<String, String> queryValues(String query) {
        Map<String, String> values = new HashMap<>();
        if (query == null) return values;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) values.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8), URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return values;
    }

    public static List<Message> publicMessages() throws IOException, InterruptedException {
        JsonObject response = post(new JsonObjectBuilder().put("action", "publicMessages").build(), sessionToken);
        return parseMessages(response, false);
    }

    public static List<Message> privateMessages(long otherUserId) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", "privateMessages");
        body.object.addProperty("otherUserId", otherUserId);
        return parseMessages(post(body.build(), sessionToken), true);
    }

    public static Long sendMessage(String content, Long receiverId, Long replyToMessageId) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", "sendMessage").put("content", content);
        if (receiverId != null) body.object.addProperty("receiverId", receiverId);
        if (replyToMessageId != null) body.object.addProperty("replyToMessageId", replyToMessageId);
        JsonObject message = post(body.build(), sessionToken).getAsJsonObject("message");
        return message.get("id").getAsLong();
    }

    public static List<User> searchUsers(String query) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", "searchUsers").put("query", query);
        List<User> users = new ArrayList<>();
        post(body.build(), sessionToken).getAsJsonArray("users").forEach(value -> users.add(parseUser(value.getAsJsonObject())));
        return users;
    }

    public static void sendFriendRequest(long receiverId) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", "sendFriendRequest");
        body.object.addProperty("receiverId", receiverId);
        post(body.build(), sessionToken);
    }

    public static void feature(String action, long id, String value) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", action);
        body.object.addProperty("messageId", id);
        if (value != null) body.object.addProperty("reaction", value);
        post(body.build(), sessionToken);
    }

    public static List<Long> pinnedMessages() throws IOException, InterruptedException {
        List<Long> ids = new ArrayList<>();
        post(new JsonObjectBuilder().put("action", "pinnedMessages").build(), sessionToken)
                .getAsJsonArray("ids").forEach(value -> ids.add(value.getAsLong()));
        return ids;
    }

    public static void markNotificationsRead() throws IOException, InterruptedException {
        post(new JsonObjectBuilder().put("action", "markNotificationsRead").build(), sessionToken);
    }

    public static List<Friend> friends() throws IOException, InterruptedException {
        List<Friend> friends = new ArrayList<>();
        post(new JsonObjectBuilder().put("action", "friends").build(), sessionToken).getAsJsonArray("friends").forEach(value -> {
            JsonObject row = value.getAsJsonObject();
            friends.add(new Friend(parseUser(row.getAsJsonObject("user")), parseTime(row.get("createdAt").getAsString())));
        });
        return friends;
    }

    public static List<FriendRequest> incomingRequests() throws IOException, InterruptedException {
        List<FriendRequest> requests = new ArrayList<>();
        post(new JsonObjectBuilder().put("action", "incomingRequests").build(), sessionToken).getAsJsonArray("requests").forEach(value -> {
            JsonObject row = value.getAsJsonObject();
            requests.add(new FriendRequest(row.get("id").getAsLong(), parseUser(row.getAsJsonObject("sender")), parseUser(row.getAsJsonObject("receiver")), row.get("status").getAsString(), parseTime(row.get("created_at").getAsString()), row.has("responded_at") && !row.get("responded_at").isJsonNull() ? parseTime(row.get("responded_at").getAsString()) : null));
        });
        return requests;
    }

    public static void respondFriendRequest(long requestId, String status) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", "respondFriendRequest").put("status", status);
        body.object.addProperty("requestId", requestId);
        post(body.build(), sessionToken);
    }

    public static void moderate(String action, long id, String content) throws IOException, InterruptedException {
        JsonObjectBuilder body = new JsonObjectBuilder().put("action", action);
        body.object.addProperty(action.equals("muteUser") || action.equals("unmuteUser") ? "targetUserId" : "messageId", id);
        if (content != null) body.object.addProperty("content", content);
        post(body.build(), sessionToken);
    }

    public static List<Notification> notifications() throws IOException, InterruptedException {
        List<Notification> notifications = new ArrayList<>();
        post(new JsonObjectBuilder().put("action", "notifications").build(), sessionToken).getAsJsonArray("notifications").forEach(value -> {
            JsonObject row = value.getAsJsonObject();
            notifications.add(new Notification(row.get("id").getAsLong(), row.get("user_id").getAsLong(), row.has("message_id") && !row.get("message_id").isJsonNull() ? row.get("message_id").getAsLong() : null, row.get("type").getAsString(), row.get("content").getAsString(), row.get("is_read").getAsBoolean(), parseTime(row.get("created_at").getAsString())));
        });
        return notifications;
    }

    private static User acceptSession(JsonObject response) {
        sessionToken = response.get("token").getAsString();
        return parseUser(response.getAsJsonObject("user"));
    }

    private static User parseUser(JsonObject source) {
        User user = new User();
        user.setId(source.get("id").getAsLong());
        user.setUsername(source.get("username").getAsString());
        if (source.has("email") && !source.get("email").isJsonNull()) user.setEmail(source.get("email").getAsString());
        if (source.has("google_id") && !source.get("google_id").isJsonNull()) user.setGoogleId(source.get("google_id").getAsString());
        if (source.has("auth_provider") && !source.get("auth_provider").isJsonNull()) user.setAuthProvider(source.get("auth_provider").getAsString());
        if (source.has("role") && !source.get("role").isJsonNull()) user.setRole(source.get("role").getAsString());
        if (source.has("status") && !source.get("status").isJsonNull()) user.setStatus(source.get("status").getAsString());
        if (source.has("created_at") && !source.get("created_at").isJsonNull()) user.setCreatedAt(parseTime(source.get("created_at").getAsString()));
        return user;
    }

    private static LocalDateTime parseTime(String value) { return LocalDateTime.parse(value.replace("Z", "")); }

    private static List<Message> parseMessages(JsonObject response, boolean privateMessages) {
        List<Message> messages = new ArrayList<>();
        response.getAsJsonArray("messages").forEach(value -> {
            JsonObject source = value.getAsJsonObject();
            long id = source.get("id").getAsLong();
            long senderId = source.get("sender_id").getAsLong();
            String sender = source.has("sender_username") && !source.get("sender_username").isJsonNull() ? source.get("sender_username").getAsString() : "Unknown";
            LocalDateTime created = LocalDateTime.parse(source.get("created_at").getAsString().replace("Z", ""));
            boolean deleted = source.has("is_deleted") && source.get("is_deleted").getAsBoolean();
            Message message = privateMessages
                    ? new PrivateMessage(id, senderId, source.get("receiver_id").getAsLong(), sender,
                    source.has("receiver_username") && !source.get("receiver_username").isJsonNull() ? source.get("receiver_username").getAsString() : "Unknown",
                    source.get("message").getAsString(), created, deleted)
                    : new PublicMessage(id, senderId, sender, source.get("message").getAsString(), created, deleted);
            if (source.has("reply_to_message_id") && !source.get("reply_to_message_id").isJsonNull()) message.setReplyToMessageId(source.get("reply_to_message_id").getAsLong());
            if (source.has("is_edited")) message.setEdited(source.get("is_edited").getAsBoolean());
            messages.add(message);
        });
        return messages;
    }

    private static JsonObject post(JsonObject body, String token) throws IOException, InterruptedException {
        String apiUrl = apiUrl();
        if (apiUrl.isBlank()) throw new IOException("SYNCSPHERE_API_URL is not configured.");
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(apiUrl + "/api/syncsphere"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(body)));
        if (token != null) request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
        if (response.statusCode() >= 400) throw new IOException(json.has("error") ? json.get("error").getAsString() : "Remote API request failed.");
        return json;
    }

    private static final class JsonObjectBuilder {
        private final JsonObject object = new JsonObject();
        JsonObjectBuilder put(String key, String value) { object.addProperty(key, value); return this; }
        JsonObject build() { return object; }
    }
}
