package xyz.quartzframework.spigot.listener;

import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.Plugin;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.listener.ListenerFactory;
import xyz.quartzframework.spigot.session.SpigotSession;

@NoProxy
@RequiredArgsConstructor
@ContextBootstrapper
public class SpigotListenerContextBootstrapper {

    @Provide
    @Preferred
    SpigotPluginEventExecutor pluginEventExecutor(SpigotSession session) {
        return new SpigotPluginEventExecutor(session);
    }

    @Provide
    @ActivateWhenBeanMissing(ListenerFactory.class)
    SpigotListenerFactory listenerFactory(Plugin plugin, SpigotPluginEventExecutor executor) {
        return new SpigotListenerFactory(plugin, executor);
    }
}