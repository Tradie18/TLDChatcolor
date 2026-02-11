package com.tldchatcolor.gui;

import com.tldchatcolor.TLDChatcolorPlugin;
import com.tldchatcolor.config.ChatColorConfig;
import com.tldchatcolor.config.Messages;
import com.tldchatcolor.data.PlayerColorStore;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Locale;
import java.util.Map;

public class HexChatListener implements Listener {
    private final TLDChatcolorPlugin plugin;
    private final HexInputTracker hexInputTracker;

    public HexChatListener(TLDChatcolorPlugin plugin, HexInputTracker hexInputTracker) {
        this.plugin = plugin;
        this.hexInputTracker = hexInputTracker;
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        if (!hexInputTracker.isAwaiting(player)) {
            return;
        }

        event.setCancelled(true);
        hexInputTracker.clear(player);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> handleHexInput(player, message));
    }

    private void handleHexInput(Player player, String message) {
        Messages messages = plugin.getMessages();
        ChatColorConfig config = plugin.getChatColorConfig();
        PlayerColorStore store = plugin.getPlayerColorStore();

        String raw = message.startsWith("#") ? message.substring(1) : message;
        String normalized = raw.toUpperCase(Locale.ROOT);

        if (!normalized.matches("[0-9A-F]{6}")) {
            player.sendMessage(messages.format("hex-invalid"));
            return;
        }

        if (config.hexBlacklist().contains(normalized)) {
            store.setColor(player.getUniqueId(), config.defaultChatColor());
            player.sendMessage(messages.format("hex-blacklisted"));
            return;
        }

        String value = "<#" + normalized + ">";
        store.setColor(player.getUniqueId(), value);
        player.sendMessage(messages.format("color-selected", Map.of(
                "color", value
        )));
    }
}
