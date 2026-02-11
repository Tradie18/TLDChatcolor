package com.tldchatcolor.gui;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HexInputTracker {
    private final Set<UUID> awaitingHex = ConcurrentHashMap.newKeySet();

    public void request(Player player) {
        awaitingHex.add(player.getUniqueId());
    }

    public boolean isAwaiting(Player player) {
        return awaitingHex.contains(player.getUniqueId());
    }

    public void clear(Player player) {
        awaitingHex.remove(player.getUniqueId());
    }
}
