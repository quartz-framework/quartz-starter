package xyz.quartzframework.spigot.event;

import lombok.RequiredArgsConstructor;
import org.bukkit.plugin.PluginManager;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.event.EventPublisher;
import xyz.quartzframework.core.task.TaskFactory;

@NoProxy
@RequiredArgsConstructor
@ContextBootstrapper
public class SpigotEventPublisherContextBootstrapper {

    private final TaskFactory taskFactory;

    private final PluginBeanDefinitionRegistry registry;

    @Provide
    @ActivateWhenBeanMissing(EventPublisher.class)
    EventPublisher eventPublisher(PluginManager pluginManager) {
        return new SpigotEventPublisher(registry, pluginManager, taskFactory);
    }
}