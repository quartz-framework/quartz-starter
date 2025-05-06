package xyz.quartzframework.bungee.session;

import lombok.RequiredArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.session.SessionService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@NoProxy
@Injectable
@RequiredArgsConstructor
@ActivateWhenBeanMissing(SessionService.class)
public class BungeeSessionService implements SessionService<CommandSender>, Listener {

    private final BungeeSession session;

    private final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> current() {
        return of(session.getSender());
    }

    @Override
    public Map<String, Object> of(CommandSender sender) {
        val senderId = session.getSenderId(sender);
        if (senderId == null) {
            return null;
        }
        return sessions.computeIfAbsent(senderId, k -> new ConcurrentHashMap<>());
    }

    @EventHandler
    private void onQuit(PlayerDisconnectEvent event) {
        val senderId = session.getSenderId(event.getPlayer());
        sessions.remove(senderId);
    }
}