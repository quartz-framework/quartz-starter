package xyz.quartzframework.spigot;

import lombok.val;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.quartzframework.core.QuartzApplicationBuilder;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.bean.strategy.BeanNameStrategy;

import java.net.URLClassLoader;

public final class SpigotQuartzBuilder extends QuartzApplicationBuilder<JavaPlugin, SpigotQuartzContext> {

    SpigotQuartzBuilder(QuartzPlugin<JavaPlugin> plugin, Class<? extends QuartzPlugin<JavaPlugin>> pluginClass) {
        super(pluginClass, plugin);
    }

    public void build() {
        val context = new SpigotQuartzContext(getPluginClass());
        run(context);
    }

    @Override
    public SpigotQuartzBuilder beanFactory(PluginBeanFactory factory) {
        super.beanFactory(factory);
        return this;
    }

    @Override
    public SpigotQuartzBuilder beanRegistry(PluginBeanDefinitionRegistry registry) {
        super.beanRegistry(registry);
        return this;
    }

    @Override
    public SpigotQuartzBuilder classLoader(URLClassLoader classLoader) {
        super.classLoader(classLoader);
        return this;
    }

    @Override
    public SpigotQuartzBuilder beanNameStrategy(BeanNameStrategy strategy) {
        super.beanNameStrategy(strategy);
        return this;
    }
}