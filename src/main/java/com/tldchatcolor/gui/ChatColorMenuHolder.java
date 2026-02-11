package com.tldchatcolor.gui;

import com.tldchatcolor.config.ChatColorConfig;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

public class ChatColorMenuHolder implements InventoryHolder {
    private final Map<Integer, ChatColorConfig.ChatColorOption> optionsBySlot;

    public ChatColorMenuHolder(Map<Integer, ChatColorConfig.ChatColorOption> optionsBySlot) {
        this.optionsBySlot = optionsBySlot;
    }

    public Map<Integer, ChatColorConfig.ChatColorOption> optionsBySlot() {
        return optionsBySlot;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
