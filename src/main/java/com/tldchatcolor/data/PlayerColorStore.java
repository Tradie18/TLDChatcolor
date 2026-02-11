package com.tldchatcolor.data;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerColorStore {
    private final File file;
    private final Logger logger;
    private final Map<UUID, PlayerStyle> stylesByPlayer;

    public PlayerColorStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        this.stylesByPlayer = new HashMap<>();
        load();
    }

    public String getColor(UUID uuid) {
        PlayerStyle style = stylesByPlayer.get(uuid);
        return style == null ? null : style.color();
    }

    public void setColor(UUID uuid, String color) {
        PlayerStyle existing = stylesByPlayer.getOrDefault(uuid, new PlayerStyle("", List.of()));
        stylesByPlayer.put(uuid, new PlayerStyle(color, existing.modifiers()));
        save();
    }

    public String getModifier(UUID uuid) {
        PlayerStyle style = stylesByPlayer.get(uuid);
        return style == null ? null : String.join("", style.modifiers());
    }

    public void setModifier(UUID uuid, String modifier) {
        PlayerStyle existing = stylesByPlayer.getOrDefault(uuid, new PlayerStyle("", List.of()));
        List<String> modifiers = modifier == null || modifier.isEmpty() ? List.of() : List.of(modifier);
        stylesByPlayer.put(uuid, new PlayerStyle(existing.color(), modifiers));
        save();
    }

    public List<String> getModifiers(UUID uuid) {
        PlayerStyle style = stylesByPlayer.get(uuid);
        return style == null ? List.of() : style.modifiers();
    }

    public void setModifiers(UUID uuid, List<String> modifiers) {
        PlayerStyle existing = stylesByPlayer.getOrDefault(uuid, new PlayerStyle("", List.of()));
        stylesByPlayer.put(uuid, new PlayerStyle(existing.color(), List.copyOf(modifiers)));
        save();
    }

    private void load() {
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                if (config.isString(key)) {
                    stylesByPlayer.put(uuid, new PlayerStyle(config.getString(key, ""), List.of()));
                    continue;
                }
                String color = config.getString(key + ".color", "");
                List<String> modifiers = config.getStringList(key + ".modifiers");
                if (modifiers.isEmpty()) {
                    String legacyModifier = config.getString(key + ".modifier", "");
                    modifiers = legacyModifier.isEmpty() ? List.of() : List.of(legacyModifier);
                }
                stylesByPlayer.put(uuid, new PlayerStyle(color, List.copyOf(modifiers)));
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid UUID in playerdata.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerStyle> entry : stylesByPlayer.entrySet()) {
            String key = entry.getKey().toString();
            PlayerStyle style = entry.getValue();
            config.set(key + ".color", style.color());
            config.set(key + ".modifiers", style.modifiers());
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            logger.warning("Failed to save playerdata.yml: " + ex.getMessage());
        }
    }

    public record PlayerStyle(String color, List<String> modifiers) {
    }
}
