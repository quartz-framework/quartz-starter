package xyz.quartzframework.bungee.listener;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import xyz.quartzframework.bungee.session.BungeeSession;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.listener.ListenerFactory;

@NoProxy
@RequiredArgsConstructor
@ContextBootstrapper
public class ListenerContextBootstrapper {

    @Provide
    @Preferred
    BungeePluginEventExecutor pluginEventExecutor(BungeeSession session) {
        return new BungeePluginEventExecutor(session);
    }

    @Provide
    @ActivateWhenBeanMissing(ListenerFactory.class)
    BungeeListenerFactory listenerFactory(Plugin plugin, PluginManager pluginManager, BungeePluginEventExecutor executor) {
        return new BungeeListenerFactory(plugin, pluginManager, executor);
    }
}