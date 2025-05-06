package xyz.quartzframework.spigot.listener;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import org.bukkit.event.Event;
import org.bukkit.plugin.EventExecutor;
import org.springframework.aop.support.AopUtils;
import xyz.quartzframework.core.listener.PluginEventExecutor;
import xyz.quartzframework.spigot.session.SpigotSession;
import xyz.quartzframework.spigot.util.EventUtil;

import java.lang.reflect.Method;

@RequiredArgsConstructor
class SpigotPluginEventExecutor implements PluginEventExecutor<EventExecutor, Event> {

    private final SpigotSession session;

    @Override
    public EventExecutor create(Object bean, Method method) {
        val eventType = method.getParameters()[0].getType();
        return (listener, event) -> {
            if (!eventType.isInstance(event)) return;
            session.runWithSender(EventUtil.getSender(event),
                    () -> triggerEvent(bean, method, event));
        };
    }

    @Override
    @SneakyThrows
    public void triggerEvent(Object bean, Method method, Event event) {
        AopUtils.invokeJoinpointUsingReflection(bean, method, new Object[]{event});
    }
}