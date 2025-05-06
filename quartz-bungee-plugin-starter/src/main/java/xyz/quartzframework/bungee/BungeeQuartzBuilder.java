package xyz.quartzframework.bungee;

import lombok.val;
import net.md_5.bungee.api.plugin.Plugin;
import xyz.quartzframework.core.QuartzApplicationBuilder;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.bean.strategy.BeanNameStrategy;

import java.net.URLClassLoader;

public final class BungeeQuartzBuilder extends QuartzApplicationBuilder<Plugin, BungeeQuartzContext> {

    BungeeQuartzBuilder(QuartzPlugin<Plugin> plugin, Class<? extends QuartzPlugin<Plugin>> pluginClass) {
        super(pluginClass, plugin);
    }

    public void build() {
        val context = new BungeeQuartzContext(getPluginClass());
        run(context);
    }

    @Override
    public BungeeQuartzBuilder beanFactory(PluginBeanFactory factory) {
        super.beanFactory(factory);
        return this;
    }

    @Override
    public BungeeQuartzBuilder beanRegistry(PluginBeanDefinitionRegistry registry) {
        super.beanRegistry(registry);
        return this;
    }

    @Override
    public BungeeQuartzBuilder classLoader(URLClassLoader classLoader) {
        super.classLoader(classLoader);
        return this;
    }

    @Override
    public BungeeQuartzBuilder beanNameStrategy(BeanNameStrategy strategy) {
        super.beanNameStrategy(strategy);
        return this;
    }
}