package com.tldchatcolor;

import com.tldchatcolor.config.ChatColorConfig;
import com.tldchatcolor.config.Messages;
import com.tldchatcolor.data.PlayerColorStore;
import com.tldchatcolor.gui.ChatColorMenuListener;
import com.tldchatcolor.placeholder.ChatColorPlaceholder;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TLDChatcolorPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    private MiniMessage miniMessage;
    private Messages messages;
    private ChatColorConfig chatColorConfig;
    private PlayerColorStore playerColorStore;

    @Override
    public void onEnable() {
        miniMessage = MiniMessage.miniMessage();

        saveResource("messages.yml", false);
        saveResource("chatcolors.yml", false);
        saveResource("playerdata.yml", false);

        messages = new Messages(getDataFolder().toPath().resolve("messages.yml").toFile(), miniMessage, getLogger());
        chatColorConfig = new ChatColorConfig(getDataFolder().toPath().resolve("chatcolors.yml").toFile(), miniMessage, getLogger());
        playerColorStore = new PlayerColorStore(getDataFolder().toPath().resolve("playerdata.yml").toFile(), getLogger());

        getCommand("chatcolor").setExecutor(this);
        getCommand("chatcolor").setTabCompleter(this);

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new ChatColorMenuListener(this), this);

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

        if (args.length > 0 && args[0].equalsIgnoreCase("sethex")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(messages.format("player-only"));
                return true;
            }
            if (!player.hasPermission("tldchatcolor.use")) {
                player.sendMessage(messages.format("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(messages.format("hex-usage"));
                return true;
            }
            String input = args[1].trim();
            handleHexCommand(player, input);
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

        player.openInventory(chatColorConfig.createMenu());
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("tldchatcolor.reload") && "reload".startsWith(args[0].toLowerCase())) {
                options.add("reload");
            }
            if ("sethex".startsWith(args[0].toLowerCase())) {
                options.add("sethex");
            }
            return options;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("sethex")) {
            return List.of("#RRGGBB");
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

    private void handleHexCommand(Player player, String input) {
        if (input.contains(" ")) {
            player.sendMessage(messages.format("hex-invalid-spaces"));
            return;
        }
        if (input.length() != 7 || input.charAt(0) != '#') {
            player.sendMessage(messages.format("hex-invalid-prefix"));
            return;
        }
        String raw = input.substring(1);
        String normalized = raw.toUpperCase(java.util.Locale.ROOT);
        if (!normalized.matches("[0-9A-F]{6}")) {
            player.sendMessage(messages.format("hex-invalid"));
            return;
        }
        if (chatColorConfig.hexBlacklist().contains(normalized)) {
            playerColorStore.setColor(player.getUniqueId(), chatColorConfig.defaultChatColor());
            player.sendMessage(messages.format("hex-blacklisted"));
            return;
        }
        String value = "<#" + normalized + ">";
        playerColorStore.setColor(player.getUniqueId(), value);
        player.sendMessage(messages.format("color-selected", java.util.Map.of(
                "color", value
        )));
    }
}
