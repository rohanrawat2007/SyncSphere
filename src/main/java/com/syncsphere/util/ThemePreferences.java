package com.syncsphere.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ThemePreferences {
    private static final Path FILE = Path.of(System.getProperty("user.home"), ".syncsphere-theme");

    private ThemePreferences() { }

    public static boolean isDark() {
        try { return !"light".equalsIgnoreCase(Files.readString(FILE, StandardCharsets.UTF_8).trim()); }
        catch (IOException ignored) { return true; }
    }

    public static void save(boolean dark) {
        try { Files.writeString(FILE, dark ? "dark" : "light", StandardCharsets.UTF_8); }
        catch (IOException ignored) { }
    }
}