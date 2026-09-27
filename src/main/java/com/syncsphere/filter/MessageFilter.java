package com.syncsphere.filter;

public interface MessageFilter {
    boolean containsBlockedContent(String message);
    String sanitize(String message);
}
