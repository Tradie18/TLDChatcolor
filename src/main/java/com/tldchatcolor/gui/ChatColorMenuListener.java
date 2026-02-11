package com.tldchatcolor.gui;

import com.tldchatcolor.TLDChatcolorPlugin;
import com.tldchatcolor.config.ChatColorConfig;
import com.tldchatcolor.config.Messages;
import com.tldchatcolor.data.PlayerColorStore;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class ChatColorMenuListener implements Listener {
    private final TLDChatcolorPlugin plugin;

    public ChatColorMenuListener(TLDChatcolorPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ChatColorMenuHolder menuHolder)) {
            return;
        }

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getView().getTopInventory())) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) {
            return;
        }

        Map<Integer, ChatColorConfig.ChatColorOption> options = menuHolder.optionsBySlot();
        ChatColorConfig.ChatColorOption option = options.get(event.getSlot());
        if (option == null) {
            return;
        }

        Messages messages = plugin.getMessages();
        ChatColorConfig config = plugin.getChatColorConfig();
        PlayerColorStore store = plugin.getPlayerColorStore();

        switch (option.type()) {
            case HEX -> {
                player.sendMessage(messages.format("hex-command"));
                player.closeInventory();
            }
            case CLEAR -> {
                store.setColor(player.getUniqueId(), config.defaultChatColor());
                store.setModifiers(player.getUniqueId(), java.util.List.of());
                player.sendMessage(messages.format("color-cleared"));
                player.closeInventory();
            }
            case MODIFIER -> {
                if (!player.hasPermission("tldchatcolor.modifier")) {
                    player.sendMessage(messages.format("no-permission-modifier"));
                    return;
                }
                java.util.List<String> current = new java.util.ArrayList<>(store.getModifiers(player.getUniqueId()));
                if (current.contains(option.chatColor())) {
                    current.remove(option.chatColor());
                    store.setModifiers(player.getUniqueId(), current);
                    player.sendMessage(messages.format("modifier-cleared"));
                } else {
                    current.add(option.chatColor());
                    store.setModifiers(player.getUniqueId(), current);
                    player.sendMessage(messages.format("modifier-selected", Map.of(
                            "modifier", option.chatColor()
                    )));
                }
                player.closeInventory();
            }
            case COLOR -> {
                store.setColor(player.getUniqueId(), option.chatColor());
                player.sendMessage(messages.format("color-selected", Map.of(
                        "color", option.chatColor()
                )));
                player.closeInventory();
            }
        }
    }
}
