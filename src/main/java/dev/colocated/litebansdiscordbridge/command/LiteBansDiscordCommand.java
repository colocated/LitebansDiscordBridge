package dev.colocated.litebansdiscordbridge.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.colocated.litebansdiscordbridge.LiteBansDiscordBridge;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class LiteBansDiscordCommand implements SimpleCommand {

    private final LiteBansDiscordBridge plugin;

    public LiteBansDiscordCommand(LiteBansDiscordBridge plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        var source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("LiteBansDiscordBridge v1.0.0", NamedTextColor.GOLD));
            source.sendMessage(Component.text("Use /litebansdiscord reload to reload the configuration", NamedTextColor.GRAY));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!source.hasPermission("litebansdiscord.reload")) {
                source.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                return;
            }

            try {
                plugin.getConfigManager().reload();
                source.sendMessage(Component.text("Configuration reloaded successfully!", NamedTextColor.GREEN));
                plugin.getLogger().info("Configuration reloaded by " + getSourceName(source));
            } catch (Exception e) {
                source.sendMessage(Component.text("Failed to reload configuration! Check console for errors.", NamedTextColor.RED));
                plugin.getLogger().error("Failed to reload configuration", e);
            }
        } else {
            source.sendMessage(Component.text("Unknown subcommand. Use /litebansdiscord reload", NamedTextColor.RED));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (args.length == 0 || args.length == 1) {
            return List.of("reload");
        }

        return List.of();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("litebansdiscord.command");
    }

    private String getSourceName(Object source) {
        if (source instanceof Player player) {
            return player.getUsername();
        }
        return "Console";
    }
}
