package com.tldchatcolor.data;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PlayerColorStore {
    private final File file;
    private final Logger logger;
    private final Map<UUID, String> colorsByPlayer;

    public PlayerColorStore(File file, Logger logger) {
        this.file = file;
        this.logger = logger;
        this.colorsByPlayer = new HashMap<>();
        load();
    }

    public String getColor(UUID uuid) {
        return colorsByPlayer.get(uuid);
    }

    public void setColor(UUID uuid, String color) {
        colorsByPlayer.put(uuid, color);
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
                colorsByPlayer.put(uuid, config.getString(key, ""));
            } catch (IllegalArgumentException ex) {
                logger.warning("Invalid UUID in playerdata.yml: " + key);
            }
        }
    }

    private void save() {
        YamlConfiguration config = new YamlConfiguration();
        for (Map.Entry<UUID, String> entry : colorsByPlayer.entrySet()) {
            config.set(entry.getKey().toString(), entry.getValue());
        }
        try {
            config.save(file);
        } catch (IOException ex) {
            logger.warning("Failed to save playerdata.yml: " + ex.getMessage());
        }
    }
}
