package xyz.quartzframework.spigot.session;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import xyz.quartzframework.core.session.SenderSession;

import java.util.UUID;

@RequiredArgsConstructor
public class SpigotSession extends SenderSession<CommandSender, Player> {

    private final Server server;

    @Override
    public Player getPlayer() {
        return getSender() instanceof Player ? (Player) getSender() : null;
    }

    @Override
    public String getSenderId(CommandSender sender) {
        if (sender == null) {
            return null;
        }
        if (!(sender instanceof OfflinePlayer player)) {
            return CONSOLE_SENDER_ID;
        }
        return server.getOnlineMode() ? player.getUniqueId().toString() : StringUtils.lowerCase(player.getName());
    }

    @Override
    public CommandSender getSenderFromId(String id) {
        if (id == null) {
            return null;
        }
        if (CONSOLE_SENDER_ID.equals(id)) {
            return server.getConsoleSender();
        }
        if (id.length() <= 16) {
            return server.getPlayer(id);
        }
        return server.getPlayer(UUID.fromString(id));
    }
}