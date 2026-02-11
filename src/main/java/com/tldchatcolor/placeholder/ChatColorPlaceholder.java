package com.tldchatcolor.placeholder;

import com.tldchatcolor.TLDChatcolorPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ChatColorPlaceholder extends PlaceholderExpansion {
    private final TLDChatcolorPlugin plugin;

    public ChatColorPlaceholder(TLDChatcolorPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "chatcolor";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        String stored = plugin.getPlayerColorStore().getColor(player.getUniqueId());
        String modifier = plugin.getPlayerColorStore().getModifier(player.getUniqueId());
        String resolvedColor = (stored == null || stored.isEmpty())
                ? plugin.getChatColorConfig().defaultChatColor()
                : stored;
        if (modifier == null || modifier.isEmpty()) {
            return resolvedColor;
        }
        return modifier + resolvedColor;
    }
}
