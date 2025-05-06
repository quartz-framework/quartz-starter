package xyz.quartzframework.bungee.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.md_5.bungee.api.plugin.Event;
import net.md_5.bungee.api.plugin.PluginManager;
import org.springframework.aop.support.AopUtils;
import xyz.quartzframework.core.bean.PluginBeanDefinition;
import xyz.quartzframework.core.bean.registry.PluginBeanDefinitionRegistry;
import xyz.quartzframework.core.event.EventPublisher;
import xyz.quartzframework.core.task.TaskFactory;
import xyz.quartzframework.core.util.InjectionUtil;

@Slf4j
@RequiredArgsConstructor
public class BungeeEventPublisher implements EventPublisher {

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
                    val instance = InjectionUtil.unwrapIfProxy(definition.getInstance());
                    if (instance == null) return;
                    definition
                            .getListenMethods()
                            .stream()
                            .filter(m -> m.getParameterCount() == 1)
                            .filter(m -> m.getParameterTypes()[0].isAssignableFrom(event.getClass()))
                            .forEach(listener -> {
                                try {
                                    if (async) {
                                        taskFactory.submit("default", () -> {
                                            try {
                                                return AopUtils.invokeJoinpointUsingReflection(instance, listener, new Object[]{event});
                                            } catch (Throwable e) {
                                                throw new RuntimeException("Unexpected error while invoking @Listen method for internal event", e);
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