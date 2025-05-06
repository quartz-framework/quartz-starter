package xyz.quartzframework.spigot.session;

import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
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
public class SpigotSessionService implements SessionService<CommandSender>, Listener {

    private final SpigotSession session;

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
    private void onQuit(PlayerQuitEvent event) {
        val senderId = session.getSenderId(event.getPlayer());
        sessions.remove(senderId);
    }
}