package com.syncsphere.thread;

import com.syncsphere.model.User;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserSession {
    private final User user;
    private final ExecutorService executorService;

    public UserSession(User user) {
        this.user = user;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "user-session-" + user.getUsername());
            thread.setDaemon(true);
            return thread;
        });
    }

    public User getUser() {
        return user;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void shutdown() {
        executorService.shutdownNow();
    }
}
