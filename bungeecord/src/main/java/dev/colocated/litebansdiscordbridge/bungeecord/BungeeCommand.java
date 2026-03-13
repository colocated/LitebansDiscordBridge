package dev.colocated.litebansdiscordbridge.bungeecord;

import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Arrays;
import java.util.Collections;

public class BungeeCommand extends Command implements TabExecutor {

    private final ConfigManager configManager;
    private final PlatformAdapter platform;

    public BungeeCommand(ConfigManager configManager, PlatformAdapter platform) {
        super("litebansdiscord", "litebansdiscord.command", "ldb", "litebansbridge");
        this.configManager = configManager;
        this.platform = platform;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(new TextComponent(ChatColor.GOLD + "LiteBansDiscordBridge"));
            sender.sendMessage(new TextComponent(ChatColor.GRAY + "Use /litebansdiscord reload to reload the configuration."));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("litebansdiscord.reload")) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "You don't have permission to use this command!"));
                return;
            }
            try {
                configManager.reload();
                sender.sendMessage(new TextComponent(ChatColor.GREEN + "Configuration reloaded successfully!"));
                platform.logInfo("Configuration reloaded by " + sender.getName());
            } catch (Exception e) {
                sender.sendMessage(new TextComponent(ChatColor.RED + "Failed to reload configuration! Check console for errors."));
                platform.logError("Failed to reload configuration", e);
            }
            return;
        }

        sender.sendMessage(new TextComponent(ChatColor.RED + "Unknown subcommand. Use /litebansdiscord reload"));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender.hasPermission("litebansdiscord.reload")) {
            return Arrays.asList("reload");
        }
        return Collections.emptyList();
    }
}
