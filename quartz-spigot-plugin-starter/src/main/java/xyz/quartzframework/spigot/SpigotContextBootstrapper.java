package xyz.quartzframework.spigot;

import lombok.RequiredArgsConstructor;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import xyz.quartzframework.core.QuartzApplication;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;

@NoProxy
@ContextBootstrapper
@RequiredArgsConstructor
public class SpigotContextBootstrapper {

    private final SpigotQuartzContext context;

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
    Server server(Plugin plugin) {
        return plugin.getServer();
    }

    @Provide
    @Preferred
    BukkitScheduler bukkitScheduler(Server server) {
        return server.getScheduler();
    }

    @Provide
    @Preferred
    PluginManager pluginManager(Server server) {
        return server.getPluginManager();
    }

    @Provide
    @Preferred
    PluginLoader pluginLoader(Plugin plugin) {
        return plugin.getPluginLoader();
    }
}