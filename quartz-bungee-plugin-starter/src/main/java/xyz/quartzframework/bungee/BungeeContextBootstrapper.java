package xyz.quartzframework.bungee;

import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;
import net.md_5.bungee.api.scheduler.TaskScheduler;
import xyz.quartzframework.core.QuartzApplication;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;

@NoProxy
@ContextBootstrapper
@RequiredArgsConstructor
public class BungeeContextBootstrapper {

    private final BungeeQuartzContext context;

    @Provide
    @Preferred
    QuartzApplication application() {
        return context.getQuartzApplication();
    }

    @Provide
    @Preferred
    Plugin plugin() {
        return context.getQuartzPlugin().getPlugin();
    }

    @Provide
    @Preferred
    ProxyServer server(Plugin plugin) {
        return plugin.getProxy();
    }

    @Provide
    @Preferred
    TaskScheduler bukkitScheduler(ProxyServer server) {
        return server.getScheduler();
    }

    @Provide
    @Preferred
    PluginManager pluginManager(ProxyServer server) {
        return server.getPluginManager();
    }
}