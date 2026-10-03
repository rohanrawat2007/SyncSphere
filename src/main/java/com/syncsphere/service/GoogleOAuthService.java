package com.syncsphere.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.syncsphere.dao.UserDAO;
import com.syncsphere.database.DBConnection;
import com.syncsphere.model.User;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class GoogleOAuthService {
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String SCOPES = "openid https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20);
    private static final int CALLBACK_TIMEOUT_SECONDS = 180;

    private final UserDAO userDAO = new UserDAO();
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(HTTP_TIMEOUT).build();

    public boolean isConfigured() {
        if (RemoteApiClient.isConfigured()) return true;
        String enabled = DBConnection.resolveConfigValue("GOOGLE_OAUTH_ENABLED", "false");
        String clientId = DBConnection.resolveConfigValue("GOOGLE_CLIENT_ID", "");
        String clientSecret = DBConnection.resolveConfigValue("GOOGLE_CLIENT_SECRET", "");
        return "true".equalsIgnoreCase(enabled)
                && isRealValue(clientId, "YOUR_GOOGLE_CLIENT_ID")
                && isRealValue(clientSecret, "YOUR_GOOGLE_CLIENT_SECRET");
    }

    public User authenticateInBrowser() throws IOException, InterruptedException {
        if (RemoteApiClient.isConfigured()) return RemoteApiClient.googleLogin();
        if (!isConfigured()) {
            throw new IllegalStateException("Google login is not configured. Please use username/password login.");
        }

        String clientId = DBConnection.resolveConfigValue("GOOGLE_CLIENT_ID", "");
        String clientSecret = DBConnection.resolveConfigValue("GOOGLE_CLIENT_SECRET", "");
        String state = randomToken(32);
        String codeVerifier = randomToken(48);
        String codeChallenge = base64Url(sha256(codeVerifier));

        try (ServerSocket callbackServer = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            callbackServer.setSoTimeout(CALLBACK_TIMEOUT_SECONDS * 1000);
            String redirectUri = "http://127.0.0.1:" + callbackServer.getLocalPort() + "/oauth2/callback";
            openBrowser(buildAuthorizationUrl(clientId, redirectUri, state, codeChallenge));
            Callback callback = waitForCallback(callbackServer, state);
            String accessToken = exchangeCode(callback.code(), codeVerifier, redirectUri, clientId, clientSecret);
            JsonObject identity = fetchIdentity(accessToken);
            String googleId = requiredIdentityValue(identity, "sub");
            String email = requiredIdentityValue(identity, "email");
            String username = identity.has("name") ? identity.get("name").getAsString() : email.substring(0, email.indexOf('@'));
            return loginOrCreateGoogleUser(googleId, email, username);
        } catch (java.net.SocketTimeoutException e) {
            throw new IllegalStateException("Google login timed out. Please try again.", e);
        }
    }

    public User loginOrCreateGoogleUser(String googleId, String email, String username) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google login is not configured. Please use username/password login.");
        }
        if (googleId == null || googleId.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google identity is incomplete.");
        }

        User existingUser = userDAO.findByGoogleId(googleId);
        if (existingUser != null) {
            return userDAO.linkGoogleIdentity(existingUser, googleId);
        }

        existingUser = userDAO.findByEmail(email);
        if (existingUser != null) {
            return userDAO.linkGoogleIdentity(existingUser, googleId);
        }

        User user = new User();
        user.setUsername(uniqueUsername(username, email));
        user.setEmail(email);
        user.setGoogleId(googleId);
        user.setAuthProvider("GOOGLE");
        user.setRole("USER");
        user.setStatus("OFFLINE");
        return userDAO.save(user);
    }

    private String uniqueUsername(String displayName, String email) {
        String base = displayName == null || displayName.isBlank() ? email.substring(0, email.indexOf('@')) : displayName;
        base = base.replaceAll("[^A-Za-z0-9_]+", "_").replaceAll("^_+|_+$", "");
        if (base.isBlank()) {
            base = "google_user";
        }
        base = base.substring(0, Math.min(base.length(), 42));
        String candidate = base;
        int suffix = 2;
        while (userDAO.findByUsername(candidate) != null) {
            String ending = "_" + suffix++;
            candidate = base.substring(0, Math.min(base.length(), 50 - ending.length())) + ending;
        }
        return candidate;
    }

    private String buildAuthorizationUrl(String clientId, String redirectUri, String state, String codeChallenge) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("client_id", clientId);
        parameters.put("redirect_uri", redirectUri);
        parameters.put("response_type", "code");
        parameters.put("scope", SCOPES);
        parameters.put("state", state);
        parameters.put("code_challenge", codeChallenge);
        parameters.put("code_challenge_method", "S256");
        parameters.put("access_type", "online");
        parameters.put("prompt", "select_account");
        return AUTHORIZATION_ENDPOINT + "?" + formEncode(parameters);
    }

    private Callback waitForCallback(ServerSocket callbackServer, String expectedState) throws IOException {
        try (var socket = callbackServer.accept()) {
            BufferedReader requestReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String requestLine = requestReader.readLine();
            if (requestLine == null) {
                throw new IOException("OAuth callback request was empty.");
            }
            int queryStart = requestLine.indexOf("GET /oauth2/callback?");
            int queryEnd = requestLine.indexOf(" HTTP/", queryStart);
            if (queryStart < 0 || queryEnd < 0) {
                sendCallbackResponse(socket, "Google login could not be completed.");
                throw new IOException("Invalid OAuth callback request.");
            }
            String query = requestLine.substring(queryStart + "GET /oauth2/callback?".length(), queryEnd);
            Map<String, String> values = parseQuery(query);
            if (!expectedState.equals(values.get("state"))) {
                sendCallbackResponse(socket, "Google login could not be verified.");
                throw new IOException("OAuth state validation failed.");
            }
            if (values.containsKey("error")) {
                sendCallbackResponse(socket, "Google login was cancelled.");
                throw new IllegalStateException("Google authorization was not completed.");
            }
            String code = values.get("code");
            if (code == null || code.isBlank()) {
                sendCallbackResponse(socket, "Google login could not be completed.");
                throw new IOException("Authorization code was not returned.");
            }
            sendCallbackResponse(socket, "Google login complete. You can return to SyncSphere.");
            return new Callback(code);
        }
    }

    private String exchangeCode(String code, String codeVerifier, String redirectUri, String clientId, String clientSecret)
            throws IOException, InterruptedException {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("code", code);
        parameters.put("client_id", clientId);
        parameters.put("client_secret", clientSecret);
        parameters.put("redirect_uri", redirectUri);
        parameters.put("grant_type", "authorization_code");
        parameters.put("code_verifier", codeVerifier);
        HttpRequest request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT)).timeout(HTTP_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formEncode(parameters))).build();
        JsonObject response = gson.fromJson(httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body(), JsonObject.class);
        if (response == null || !response.has("access_token")) {
            throw new IOException("Google token exchange failed.");
        }
        return response.get("access_token").getAsString();
    }

    private JsonObject fetchIdentity(String accessToken) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(USERINFO_ENDPOINT)).timeout(HTTP_TIMEOUT)
                .header("Authorization", "Bearer " + accessToken).GET().build();
        JsonObject identity = gson.fromJson(httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body(), JsonObject.class);
        if (identity == null || !identity.has("sub") || !identity.has("email")) {
            throw new IOException("Google account information is unavailable.");
        }
        return identity;
    }

    private void openBrowser(String authorizationUrl) throws IOException {
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            throw new IOException("A default browser is unavailable.");
        }
        Desktop.getDesktop().browse(URI.create(authorizationUrl));
    }

    private void sendCallbackResponse(java.net.Socket socket, String message) throws IOException {
        byte[] body = ("<html><body><h2>" + message + "</h2><p>You may close this tab.</p></body></html>")
                .getBytes(StandardCharsets.UTF_8);
        String headers = "HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: "
                + body.length + "\r\nConnection: close\r\n\r\n";
        OutputStream output = socket.getOutputStream();
        output.write(headers.getBytes(StandardCharsets.UTF_8));
        output.write(body);
        output.flush();
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2) {
                values.put(urlDecode(parts[0]), urlDecode(parts[1]));
            }
        }
        return values;
    }

    private String formEncode(Map<String, String> values) {
        return values.entrySet().stream().map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .reduce((left, right) -> left + "&" + right).orElse("");
    }

    private String requiredIdentityValue(JsonObject identity, String key) {
        String value = identity.get(key).getAsString();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Google account information is unavailable.");
        }
        return value;
    }

    private String randomToken(int bytes) {
        byte[] value = new byte[bytes];
        new SecureRandom().nextBytes(value);
        return base64Url(value);
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            throw new IllegalStateException("Secure OAuth setup failed.", e);
        }
    }

    private String base64Url(byte[] value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
    private String urlEncode(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private String urlDecode(String value) { return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8); }
    private boolean isRealValue(String value, String placeholder) { return value != null && !value.isBlank() && !placeholder.equals(value); }
    private record Callback(String code) { }
}
