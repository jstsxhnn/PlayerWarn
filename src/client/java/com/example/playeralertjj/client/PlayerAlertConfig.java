package com.example.playeralertjj.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerAlertConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("playeralertjj");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int version = 1;
    private Set<String> ignoredPlayers = new HashSet<>();

    public static PlayerAlertConfig load(Path path) {
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                PlayerAlertConfig config = GSON.fromJson(reader, PlayerAlertConfig.class);
                if (config != null) {
                    config.normalize();
                    return config;
                }
            } catch (IOException | JsonSyntaxException e) {
                LOGGER.warn("Konnte playeralertjj.json nicht lesen, nutze Standardwerte.", e);
            }
        }
        return new PlayerAlertConfig();
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            LOGGER.warn("Konnte playeralertjj.json nicht speichern.", e);
        }
    }

    public boolean isIgnored(String name) {
        String normalized = normalizeName(name);
        return !normalized.isEmpty() && ignoredPlayers.contains(normalized);
    }

    public boolean addIgnored(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) {
            return false;
        }
        return ignoredPlayers.add(normalized);
    }

    public boolean removeIgnored(String name) {
        String normalized = normalizeName(name);
        if (normalized.isEmpty()) {
            return false;
        }
        return ignoredPlayers.remove(normalized);
    }

    public void clearIgnored() {
        ignoredPlayers.clear();
    }

    public Set<String> getIgnoredPlayers() {
        return Collections.unmodifiableSet(ignoredPlayers);
    }

    private void normalize() {
        if (ignoredPlayers == null) {
            ignoredPlayers = new HashSet<>();
            return;
        }
        Set<String> normalized = new HashSet<>();
        for (String name : ignoredPlayers) {
            String cleaned = normalizeName(name);
            if (!cleaned.isEmpty()) {
                normalized.add(cleaned);
            }
        }
        ignoredPlayers = normalized;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
