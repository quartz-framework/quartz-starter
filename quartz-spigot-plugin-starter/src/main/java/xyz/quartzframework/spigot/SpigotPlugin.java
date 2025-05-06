package xyz.quartzframework.spigot;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.context.AbstractQuartzContext;

@NoProxy
public abstract class SpigotPlugin extends JavaPlugin implements QuartzPlugin<JavaPlugin> {

    @Getter
    @Setter
    private AbstractQuartzContext<JavaPlugin> context;

    public static SpigotQuartzBuilder builder(SpigotPlugin plugin) {
        return new SpigotQuartzBuilder(plugin, plugin.getClass());
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
    public JavaPlugin getPlugin() {
        return this;
    }

}