package dev.colocated.litebansdiscordbridge.velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.colocated.litebansdiscordbridge.config.ConfigManager;
import dev.colocated.litebansdiscordbridge.platform.PlatformAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VelocityCommand implements SimpleCommand {

    private final ConfigManager configManager;
    private final PlatformAdapter platform;

    public VelocityCommand(ConfigManager configManager, PlatformAdapter platform) {
        this.configManager = configManager;
        this.platform = platform;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("LiteBansDiscordBridge", NamedTextColor.GOLD));
            source.sendMessage(Component.text("Use /litebansdiscord reload to reload the configuration.", NamedTextColor.GRAY));
            return;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!source.hasPermission("litebansdiscord.reload")) {
                source.sendMessage(Component.text("You don't have permission to use this command!", NamedTextColor.RED));
                return;
            }
            try {
                configManager.reload();
                source.sendMessage(Component.text("Configuration reloaded successfully!", NamedTextColor.GREEN));
                platform.logInfo("Configuration reloaded by " + getSourceName(source));
            } catch (Exception e) {
                source.sendMessage(Component.text("Failed to reload configuration! Check console for errors.", NamedTextColor.RED));
                platform.logError("Failed to reload configuration", e);
            }
            return;
        }

        source.sendMessage(Component.text("Unknown subcommand. Use /litebansdiscord reload", NamedTextColor.RED));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 0 || args.length == 1) {
            return Arrays.asList("reload");
        }
        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("litebansdiscord.command");
    }

    private String getSourceName(CommandSource source) {
        if (source instanceof Player) {
            return ((Player) source).getUsername();
        }
        return "Console";
    }
}
