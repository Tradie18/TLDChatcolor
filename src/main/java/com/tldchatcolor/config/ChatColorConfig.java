package com.tldchatcolor.config;

import com.tldchatcolor.gui.ChatColorMenuHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
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
    private final String guiTitle;
    private final List<String> selectedLore;
    private final StatusOption statusOption;
    private final String defaultChatColor;
    private final Set<String> hexBlacklist;
    private final FillerOption fillerOption;
    private final Map<Integer, ChatColorOption> optionsBySlot;

    public ChatColorConfig(File file, MiniMessage miniMessage, Logger logger) {
        this.miniMessage = miniMessage;
        this.logger = logger;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        this.guiSize = normalizeSize(config.getInt("gui.size", 27));
        this.guiTitle = config.getString("gui.title", "<green>Select a Chat Color</green>");
        this.selectedLore = loadSelectedLore(config);
        this.defaultChatColor = config.getString("default-color", "<white>");
        this.hexBlacklist = loadHexBlacklist(config.getStringList("hex-blacklist"));
        this.fillerOption = loadFillerOption(config.getConfigurationSection("gui.filler"));
        this.statusOption = loadStatusOption(config.getConfigurationSection("gui.status"));
        this.optionsBySlot = loadOptions(config);
    }

    public Inventory createMenu(Player player, String selectedColor, List<String> modifiers) {
        ChatColorMenuHolder holder = new ChatColorMenuHolder(optionsBySlot);
        Inventory inventory = Bukkit.createInventory(holder, guiSize, miniMessage.deserialize(guiTitle));

        for (Map.Entry<Integer, ChatColorOption> entry : optionsBySlot.entrySet()) {
            ChatColorOption option = entry.getValue();
            ItemStack item = new ItemStack(option.material());
            ItemMeta meta = item.getItemMeta();
            if (option.type() == OptionType.MODIFIER && !player.hasPermission("tldchatcolor.modifier")) {
                meta.displayName(miniMessage.deserialize("<red>No permission</red>"));
            } else {
                meta.displayName(miniMessage.deserialize(option.displayName()));
                List<Component> lore = new ArrayList<>();
                if (option.type() == OptionType.MODIFIER) {
                    boolean selected = modifiers.contains(option.chatColor());
                    for (String line : option.lore()) {
                        String resolved = line.replace("{selected}", selected ? "Yes" : "No");
                        lore.add(miniMessage.deserialize(resolved));
                    }
                } else {
                    boolean selected = option.type() == OptionType.COLOR && option.chatColor().equals(selectedColor);
                    for (String line : option.lore()) {
                        if (selected && line.equalsIgnoreCase("<gray>Click to select.</gray>")) {
                            continue;
                        }
                        lore.add(miniMessage.deserialize(line));
                    }
                    if (selected) {
                        for (String line : selectedLore) {
                            lore.add(miniMessage.deserialize(line));
                        }
                    }
                }
                if (!lore.isEmpty()) {
                    meta.lore(lore);
                }
            }
            item.setItemMeta(meta);
            inventory.setItem(entry.getKey(), item);
        }

        if (statusOption.enabled()) {
            ItemStack statusItem = createStatusItem(selectedColor, String.join("", modifiers));
            if (statusItem != null && statusOption.slot() >= 0 && statusOption.slot() < guiSize) {
                inventory.setItem(statusOption.slot(), statusItem);
            }
        }

        if (fillerOption.enabled()) {
            ItemStack filler = createFillerItem();
            if (filler != null) {
                for (int slot = 0; slot < guiSize; slot++) {
                    if (inventory.getItem(slot) == null) {
                        inventory.setItem(slot, filler);
                    }
                }
            }
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

    public Component guiTitle() {
        return miniMessage.deserialize(guiTitle);
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

    private List<String> loadSelectedLore(YamlConfiguration config) {
        List<String> lore = config.getStringList("gui.selected-lore");
        if (lore == null || lore.isEmpty()) {
            return List.of("<green>Selected</green>");
        }
        return List.copyOf(lore);
    }

    private StatusOption loadStatusOption(ConfigurationSection section) {
        if (section == null) {
            return new StatusOption(false, Material.OAK_SIGN, "", List.of(), -1);
        }
        boolean enabled = section.getBoolean("enabled", false);
        String materialName = section.getString("material", "OAK_SIGN");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            logger.warning("Invalid status material: " + materialName);
            material = Material.OAK_SIGN;
        }
        String displayName = section.getString("display-name", "<gray>Your Selection</gray>");
        List<String> lore = section.getStringList("lore");
        int slot = section.getInt("slot", -1);
        return new StatusOption(enabled, material, displayName, lore, slot);
    }

    private ItemStack createStatusItem(String selectedColor, String modifier) {
        Material material = statusOption.material();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(miniMessage.deserialize(applyPlaceholders(statusOption.displayName(), selectedColor, modifier)));
        if (!statusOption.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : statusOption.lore()) {
                lore.add(miniMessage.deserialize(applyPlaceholders(line, selectedColor, modifier)));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String applyPlaceholders(String input, String selectedColor, String modifier) {
        String resolved = input.replace("{color}", selectedColor);
        String safeModifier = modifier == null ? "" : modifier;
        resolved = resolved.replace("{modifier}", safeModifier);
        if (resolved.contains("{preview}")) {
            String preview = safeModifier + selectedColor + "Your chat currently looks like this";
            resolved = resolved.replace("{preview}", preview);
        }
        return resolved;
    }

    private FillerOption loadFillerOption(ConfigurationSection section) {
        if (section == null) {
            return new FillerOption(false, Material.BLACK_STAINED_GLASS_PANE, "", List.of());
        }
        boolean enabled = section.getBoolean("enabled", false);
        String materialName = section.getString("material", "BLACK_STAINED_GLASS_PANE");
        Material material = Material.matchMaterial(materialName);
        if (material == null) {
            logger.warning("Invalid filler material: " + materialName);
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        String displayName = section.getString("display-name", "");
        List<String> lore = section.getStringList("lore");
        return new FillerOption(enabled, material, displayName, lore);
    }

    private ItemStack createFillerItem() {
        Material material = fillerOption.material();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (!fillerOption.displayName().isEmpty()) {
            meta.displayName(miniMessage.deserialize(fillerOption.displayName()));
        }
        if (!fillerOption.lore().isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : fillerOption.lore()) {
                lore.add(miniMessage.deserialize(line));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public enum OptionType {
        COLOR,
        CLEAR,
        HEX,
        MODIFIER;

        public static OptionType from(String raw) {
            if (raw == null) {
                return COLOR;
            }
            return switch (raw.toLowerCase(Locale.ROOT)) {
                case "clear" -> CLEAR;
                case "hex" -> HEX;
                case "modifier" -> MODIFIER;
                default -> COLOR;
            };
        }
    }

    public record ChatColorOption(String id, Material material, String displayName, List<String> lore,
                                  String chatColor, OptionType type) {
    }

    private record FillerOption(boolean enabled, Material material, String displayName, List<String> lore) {
    }

    private record StatusOption(boolean enabled, Material material, String displayName, List<String> lore, int slot) {
    }
}
