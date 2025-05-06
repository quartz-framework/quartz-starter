package xyz.quartzframework.bungee.session;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.apache.commons.lang3.StringUtils;
import xyz.quartzframework.core.session.SenderSession;

import java.util.UUID;

@RequiredArgsConstructor
public class BungeeSession extends SenderSession<CommandSender, ProxiedPlayer> {

    private final ProxyServer server;

    @Override
    public ProxiedPlayer getPlayer() {
        return getSender() instanceof ProxiedPlayer ? (ProxiedPlayer) getSender() : null;
    }

    @Override
    public String getSenderId(CommandSender sender) {
        if (sender == null) {
            return null;
        }
        if (!(sender instanceof ProxiedPlayer player)) {
            return StringUtils.lowerCase(sender.getName());
        }
        return player.getUniqueId().toString();
    }

    @Override
    public CommandSender getSenderFromId(String id) {
        if (id == null) {
            return null;
        }
        if (CONSOLE_SENDER_ID.equals(id)) {
            return server.getConsole();
        }
        if (id.length() <= 16) {
            return server.getPlayer(id);
        }
        return server.getPlayer(UUID.fromString(id));
    }
}