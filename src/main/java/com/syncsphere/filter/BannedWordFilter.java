package com.syncsphere.filter;

import java.util.List;
import java.util.Locale;

public class BannedWordFilter implements MessageFilter {
    private final List<String> bannedWords;

    public BannedWordFilter(List<String> bannedWords) {
        this.bannedWords = bannedWords == null ? List.of() : bannedWords.stream()
                .filter(word -> word != null && !word.isBlank())
                .map(word -> word.trim().toLowerCase(Locale.ROOT))
                .toList();
    }

    @Override
    public boolean containsBlockedContent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase(Locale.ROOT);
        return bannedWords.stream().anyMatch(normalized::contains);
    }

    @Override
    public String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        String sanitized = message;
        for (String bannedWord : bannedWords) {
            sanitized = sanitized.replaceAll("(?i)" + bannedWord, "[filtered]");
        }
        return sanitized;
    }
}
