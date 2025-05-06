package xyz.quartzframework.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.context.AbstractQuartzContext;

class BungeeQuartzContext extends AbstractQuartzContext<Plugin> {

    public BungeeQuartzContext(Class<? extends QuartzPlugin<Plugin>> pluginClass) {
        super(pluginClass, null, null, null, null);
    }
}