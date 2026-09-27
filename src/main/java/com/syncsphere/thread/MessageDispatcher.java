package com.syncsphere.thread;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MessageDispatcher {
    private final List<String> messageQueue = new CopyOnWriteArrayList<>();

    public void dispatch(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        synchronized (messageQueue) {
            messageQueue.add(message);
        }
    }

    public List<String> getMessages() {
        synchronized (messageQueue) {
            return List.copyOf(messageQueue);
        }
    }
}
