package xyz.quartzframework.spigot.listener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.event.*;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.springframework.aop.support.AopUtils;
import org.springframework.util.ReflectionUtils;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.event.Listen;
import xyz.quartzframework.core.listener.ListenerFactory;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

@Getter
@NoProxy
@RequiredArgsConstructor
class SpigotListenerFactory implements ListenerFactory<EventExecutor, Event> {

    private final Plugin plugin;

    private final SpigotPluginEventExecutor executor;

    public void registerEvents(Object bean) {
        getListenerMethods(bean).forEach(method -> registerEvent(bean, method));
    }

    @Override
    public void unregisterEvents(Object listener) {
        if (listener instanceof Listener l) {
            HandlerList.unregisterAll(l);
        }
    }

    @SuppressWarnings("unchecked")
    private void registerEvent(Object bean, Method method) {
        val server = plugin.getServer();
        val eventType = (Class<? extends Event>) method.getParameters()[0].getType();
        val eventHandler = method.getAnnotation(EventHandler.class);
        val priority = eventHandler != null
                ? eventHandler.priority()
                : (EventPriority.NORMAL);
        val ignoreCancelled = eventHandler == null || eventHandler.ignoreCancelled();
        Listener listener = (bean instanceof Listener) ? (Listener) bean : new Listener() {};
        server.getPluginManager().registerEvent(
                eventType,
                listener,
                priority,
                getExecutor().create(bean, method),
                plugin,
                ignoreCancelled
        );
    }

    private Stream<Method> getListenerMethods(Object bean) {
        val target = AopUtils.getTargetClass(bean);
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(target))
                .filter(method ->
                        (method.isAnnotationPresent(EventHandler.class) || method.isAnnotationPresent(Listen.class))
                                && method.getParameters().length == 1
                                && Event.class.isAssignableFrom(method.getParameters()[0].getType())
                );
    }
}