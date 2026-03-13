package dev.colocated.litebansdiscordbridge.bukkit;

import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BukkitCommand implements CommandExecutor, TabCompleter {

    private final ConfigManager configManager;
    private final PlatformAdapter platform;

    public BukkitCommand(ConfigManager configManager, PlatformAdapter platform) {
        this.configManager = configManager;
        this.platform = platform;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "LiteBansDiscordBridge");
            sender.sendMessage(ChatColor.GRAY + "Use /" + label + " reload to reload the configuration.");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("litebansdiscord.reload")) {
                sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
                return true;
            }
            try {
                configManager.reload();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
                platform.logInfo("Configuration reloaded by " + sender.getName());
            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "Failed to reload configuration! Check console for errors.");
                platform.logError("Failed to reload configuration", e);
            }
            return true;
        }

        sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /" + label + " reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("litebansdiscord.reload")) {
            return Arrays.asList("reload");
        }
        return Collections.emptyList();
    }
}
