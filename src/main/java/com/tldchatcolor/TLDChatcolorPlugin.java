package com.tldchatcolor;

import com.tldchatcolor.config.ChatColorConfig;
import com.tldchatcolor.config.Messages;
import com.tldchatcolor.data.PlayerColorStore;
import com.tldchatcolor.gui.ChatColorMenuListener;
import com.tldchatcolor.gui.HexChatListener;
import com.tldchatcolor.gui.HexInputTracker;
import com.tldchatcolor.placeholder.ChatColorPlaceholder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class TLDChatcolorPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private MiniMessage miniMessage;
    private Messages messages;
    private ChatColorConfig chatColorConfig;
    private PlayerColorStore playerColorStore;
    private HexInputTracker hexInputTracker;

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();

        saveResource("messages.yml", false);
        saveResource("chatcolors.yml", false);
        saveResource("playerdata.yml", false);

        messages = new Messages(getDataFolder().toPath().resolve("messages.yml").toFile(), miniMessage, getLogger());
        chatColorConfig = new ChatColorConfig(getDataFolder().toPath().resolve("chatcolors.yml").toFile(), miniMessage, getLogger());
        playerColorStore = new PlayerColorStore(getDataFolder().toPath().resolve("playerdata.yml").toFile(), getLogger());
        hexInputTracker = new HexInputTracker();

        getCommand("chatcolor").setExecutor(this);
        getCommand("chatcolor").setTabCompleter(this);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ChatColorMenuListener(this, hexInputTracker), this);
        pluginManager.registerEvents(new HexChatListener(this, hexInputTracker), this);

        if (pluginManager.isPluginEnabled("PlaceholderAPI")) {
            new ChatColorPlaceholder(this).register();
        } else {
            getLogger().warning("PlaceholderAPI not found. %chatcolor% will not be available.");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("tldchatcolor.reload")) {
                sender.sendMessage(messages.format("no-permission"));
                return true;
            }
            reloadConfigs();
            sender.sendMessage(messages.format("reload-complete"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.format("player-only"));
            return true;
        }

        if (!player.hasPermission("tldchatcolor.use")) {
            player.sendMessage(messages.format("no-permission"));
            return true;
        }

        player.openInventory(chatColorConfig.createMenu(messages.guiTitle()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("tldchatcolor.reload") && "reload".startsWith(args[0].toLowerCase())) {
                return List.of("reload");
            }
        }
        return Collections.emptyList();
    }

    private void reloadConfigs() {
        messages = new Messages(getDataFolder().toPath().resolve("messages.yml").toFile(), miniMessage, getLogger());
        chatColorConfig = new ChatColorConfig(getDataFolder().toPath().resolve("chatcolors.yml").toFile(), miniMessage, getLogger());
        playerColorStore = new PlayerColorStore(getDataFolder().toPath().resolve("playerdata.yml").toFile(), getLogger());
    }

    public Messages getMessages() {
        return messages;
    }

    public ChatColorConfig getChatColorConfig() {
        return chatColorConfig;
    }

    public PlayerColorStore getPlayerColorStore() {
        return playerColorStore;
    }
}
