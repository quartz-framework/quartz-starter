package xyz.quartzframework.bungee.listener;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.event.EventHandler;
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
class BungeeListenerFactory implements ListenerFactory<Listener, Event> {

    private final Plugin plugin;

    private final PluginManager pluginManager;

    private final BungeePluginEventExecutor executor;

    public void registerEvents(Object bean) {
        getListenerMethods(bean).forEach(method -> registerEvent(bean, method));
    }

    @Override
    public void unregisterEvents(Object listener) {
        if (listener instanceof Listener l) {
            pluginManager.unregisterListener(l);
        }
    }

    private void registerEvent(Object bean, Method method) {
        Listener dynamicListener = getExecutor().create(bean, method);
        pluginManager.registerListener(plugin, dynamicListener);
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