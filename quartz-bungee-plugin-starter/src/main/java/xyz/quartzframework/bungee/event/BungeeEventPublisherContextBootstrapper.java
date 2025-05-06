package xyz.quartzframework.bungee.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.plugin.PluginManager;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.event.EventPublisher;
import xyz.quartzframework.core.task.TaskFactory;

@NoProxy
@Slf4j
@RequiredArgsConstructor
@ContextBootstrapper
public class BungeeEventPublisherContextBootstrapper {

    private final TaskFactory taskFactory;

    private final PluginBeanDefinitionRegistry registry;

    @Provide
    @ActivateWhenBeanMissing(EventPublisher.class)
    EventPublisher eventPublisher(PluginManager pluginManager) {
        return new BungeeEventPublisher(registry, pluginManager, taskFactory);
    }
}