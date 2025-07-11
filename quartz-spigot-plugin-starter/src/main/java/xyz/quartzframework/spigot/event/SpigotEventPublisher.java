package xyz.quartzframework.spigot.event;

import io.github.classgraph.MethodInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.plugin.PluginManager;
import org.springframework.aop.support.AopUtils;
import xyz.quartzframework.core.bean.BeanInjector;
import xyz.quartzframework.core.bean.definition.PluginBeanDefinition;
import xyz.quartzframework.core.bean.definition.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.bean.definition.metadata.MethodMetadata;
import xyz.quartzframework.core.event.EventPublisher;
import xyz.quartzframework.core.task.TaskFactory;

@Slf4j
@RequiredArgsConstructor
public class SpigotEventPublisher implements EventPublisher {

    private final PluginBeanDefinitionRegistry registry;

    private final PluginManager pluginManager;

    private final TaskFactory taskFactory;

    private void handleInternalEvent(Event event, boolean async) {
        registry.getBeanDefinitions()
                .stream()
                .filter(PluginBeanDefinition::isInitialized)
                .filter(PluginBeanDefinition::isInjected)
                .filter(b -> !b.getListenMethods().isEmpty())
                .forEach(definition -> {
                    val instance = BeanInjector.unwrapIfProxy(definition.getInstance());
                    if (instance == null) return;
                    definition
                            .getListenMethods()
                            .stream()
                            .map(MethodMetadata::getMethod)
                            .filter(m -> m.getParameterCount() == 1)
                            .filter(m -> m.getParameterTypes()[0].isAssignableFrom(event.getClass()))
                            .forEach(listener -> {
                                try {
                                    if (async) {
                                        taskFactory.submit("default", () -> {
                                            try {
                                                return AopUtils.invokeJoinpointUsingReflection(instance, listener, new Object[]{event});
                                            } catch (Throwable e) {
                                                throw new EventException(e, "Unexpected error while invoking @Listen method for internal event");
                                            }
                                        });
                                    } else {
                                        AopUtils.invokeJoinpointUsingReflection(instance, listener, new Object[]{event});
                                    }
                                } catch (Throwable e) {
                                    log.error("Unexpected error while invoking @Listen method for internal event: ", e);
                                }
                            });
                });
    }

    @Override
    public void publish(Object event, boolean internal, boolean async) {
        if (event instanceof Event e) {
            if (internal) {
                handleInternalEvent(e, async);
            } else {
                if (async) {
                    taskFactory.submit("default", () -> pluginManager.callEvent(e));
                } else {
                    pluginManager.callEvent(e);
                }
            }
        }
    }
}