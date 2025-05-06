package xyz.quartzframework.spigot;

import org.bukkit.plugin.java.JavaPlugin;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.context.AbstractQuartzContext;

class SpigotQuartzContext extends AbstractQuartzContext<JavaPlugin> {

    public SpigotQuartzContext(Class<? extends QuartzPlugin<JavaPlugin>> pluginClass) {
        super(pluginClass, null, null, null, null);
    }
}