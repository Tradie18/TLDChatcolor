package com.tldchatcolor.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Messages {
    private final MiniMessage miniMessage;
    private final Logger logger;
    private final String prefix;
    private final Map<String, String> messages;

    public Messages(File file, MiniMessage miniMessage, Logger logger) {
        this.miniMessage = miniMessage;
        this.logger = logger;
        this.messages = new HashMap<>();

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.prefix = config.getString("prefix", "");
        for (String key : config.getKeys(false)) {
            if (key.equals("prefix")) {
                continue;
            }
            messages.put(key, config.getString(key, ""));
        }
    }

    public Component format(String key) {
        return format(key, Map.of());
    }

    public Component format(String key, Map<String, String> placeholders) {
        String raw = messages.getOrDefault(key, "");
        if (raw.isEmpty()) {
            logger.warning("Missing message key: " + key);
        }
        String resolved = applyPlaceholders(raw, placeholders);
        String withPrefix = key.equals("gui-title") ? resolved : prefix + resolved;
        return miniMessage.deserialize(withPrefix);
    }

    public Component guiTitle() {
        return format("gui-title");
    }

    private String applyPlaceholders(String message, Map<String, String> placeholders) {
        String resolved = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return resolved;
    }
}
