package com.tldchatcolor.config;

import com.tldchatcolor.gui.ChatColorMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

public class ChatColorConfig {
    private final MiniMessage miniMessage;
    private final Logger logger;
    private final int guiSize;
    private final String defaultChatColor;
    private final Set<String> hexBlacklist;
    private final Map<Integer, ChatColorOption> optionsBySlot;

    public ChatColorConfig(File file, MiniMessage miniMessage, Logger logger) {
        this.miniMessage = miniMessage;
        this.logger = logger;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.guiSize = normalizeSize(config.getInt("gui.size", 27));
        this.defaultChatColor = config.getString("default-color", "<white>");
        this.hexBlacklist = loadHexBlacklist(config.getStringList("hex-blacklist"));
        this.optionsBySlot = loadOptions(config);
    }

    public Inventory createMenu(Component title) {
        ChatColorMenuHolder holder = new ChatColorMenuHolder(optionsBySlot);
        Inventory inventory = Bukkit.createInventory(holder, guiSize, title);

        for (Map.Entry<Integer, ChatColorOption> entry : optionsBySlot.entrySet()) {
            ChatColorOption option = entry.getValue();
            ItemStack item = new ItemStack(option.material());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(miniMessage.deserialize(option.displayName()));
            if (!option.lore().isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : option.lore()) {
                    lore.add(miniMessage.deserialize(line));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
            inventory.setItem(entry.getKey(), item);
        }

        return inventory;
    }

    public Optional<ChatColorOption> getOption(int slot) {
        return Optional.ofNullable(optionsBySlot.get(slot));
    }

    public String defaultChatColor() {
        return defaultChatColor;
    }

    public Set<String> hexBlacklist() {
        return hexBlacklist;
    }

    private Map<Integer, ChatColorOption> loadOptions(YamlConfiguration config) {
        Map<Integer, ChatColorOption> options = new HashMap<>();
        ConfigurationSection colorsSection = config.getConfigurationSection("colors");
        if (colorsSection == null) {
            return options;
        }

        for (String key : colorsSection.getKeys(false)) {
            ConfigurationSection section = colorsSection.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String materialName = section.getString("material", "WHITE_WOOL");
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                logger.warning("Invalid material for color " + key + ": " + materialName);
                continue;
            }

            String displayName = section.getString("display-name", key);
            List<String> lore = section.getStringList("lore");
            String typeRaw = section.getString("type", "color");
            OptionType type = OptionType.from(typeRaw);
            String chatColor = section.getString("chat-color", "");
            int slot = section.getInt("slot", -1);
            if (slot < 0 || slot >= guiSize) {
                logger.warning("Invalid slot for color " + key + ": " + slot);
                continue;
            }

            options.put(slot, new ChatColorOption(key, material, displayName, lore, chatColor, type));
        }

        return options;
    }

    private int normalizeSize(int size) {
        int normalized = Math.max(9, Math.min(54, size));
        if (normalized % 9 != 0) {
            normalized = ((normalized / 9) + 1) * 9;
        }
        return normalized;
    }

    private Set<String> loadHexBlacklist(List<String> rawList) {
        Set<String> normalized = new HashSet<>();
        for (String raw : rawList) {
            if (raw == null) {
                continue;
            }
            String value = raw.replace("#", "").trim().toUpperCase(Locale.ROOT);
            if (!value.isEmpty()) {
                normalized.add(value);
            }
        }
        return Collections.unmodifiableSet(normalized);
    }

    public enum OptionType {
        COLOR,
        CLEAR,
        HEX;

        public static OptionType from(String raw) {
            if (raw == null) {
                return COLOR;
            }
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "clear" -> CLEAR;
                case "hex" -> HEX;
                default -> COLOR;
            };
        }
    }

    public record ChatColorOption(String id, Material material, String displayName, List<String> lore,
                                  String chatColor, OptionType type) {
    }
}
