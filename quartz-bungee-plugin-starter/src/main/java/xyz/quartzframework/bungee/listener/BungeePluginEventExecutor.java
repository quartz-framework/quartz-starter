package xyz.quartzframework.bungee.listener;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.springframework.aop.support.AopUtils;
import xyz.quartzframework.bungee.session.BungeeSession;
import xyz.quartzframework.bungee.util.EventUtil;
import xyz.quartzframework.core.listener.PluginEventExecutor;

import java.lang.reflect.Method;

@Slf4j
@RequiredArgsConstructor
public class BungeePluginEventExecutor implements PluginEventExecutor<Listener, Event> {

    private final BungeeSession session;

    @Override
    public Listener create(Object bean, Method method) {
        val eventType = method.getParameterTypes()[0];

        return new Listener() {

            @EventHandler
            public void onEvent(Event event) {
                if (!eventType.isInstance(event)) return;
                val sender = EventUtil.getSender(event);
                session.runWithSender(sender, () -> {
                    try {
                        triggerEvent(bean, method, event);
                    } catch (Throwable t) {
                        log.warn("Failed to handle event {}: {}", event.getClass().getSimpleName(), t.getMessage(), t);
                    }
                });
            }
        };
    }

    @SneakyThrows
    @Override
    public void triggerEvent(Object bean, Method method, Event event) {
        AopUtils.invokeJoinpointUsingReflection(bean, method, new Object[]{event});
    }
}