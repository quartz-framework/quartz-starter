package xyz.quartzframework.bungee;

import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.context.AbstractQuartzContext;

@NoProxy
public abstract class BungeePlugin extends Plugin implements QuartzPlugin<Plugin> {

    @Getter
    @Setter
    private AbstractQuartzContext<Plugin> context;

    public static BungeeQuartzBuilder builder(BungeePlugin plugin) {
        return new BungeeQuartzBuilder(plugin, plugin.getClass());
    }

    @Override
    public final void onLoad() {

    }

    @Override
    public final void onEnable() {
        main();
    }

    @Override
    public final void onDisable() {
        close();
    }

    @Override
    public Plugin getPlugin() {
        return this;
    }

}