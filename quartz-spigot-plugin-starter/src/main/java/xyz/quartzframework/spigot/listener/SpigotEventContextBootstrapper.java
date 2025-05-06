package xyz.quartzframework.spigot.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.event.Listener;
import xyz.quartzframework.core.bean.annotation.Inject;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.util.InjectionUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@NoProxy
@ContextBootstrapper
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SpigotEventContextBootstrapper {

    private final PluginBeanFactory pluginBeanFactory;

    private final SpigotListenerFactory listenerFactory;

    private final List<Listener> events;

    private final List<Listener> dynamicListeners = new ArrayList<>();

    @PreDestroy
    public void onDestroy() {
        dynamicListeners.forEach(listenerFactory::unregisterEvents);
        dynamicListeners.clear();
    }

    @PostConstruct
    public void postConstruct() {
        events.forEach(listener -> listenerFactory.registerEvents(InjectionUtil.unwrapIfProxy(listener)));
        val registry = pluginBeanFactory.getRegistry();
        registry
                .getBeanDefinitions()
                .stream()
                .filter(def -> !def.getListenMethods().isEmpty())
                .forEach(def -> {
                    val bean = pluginBeanFactory.getBean(def.getName(), def.getType());
                    val realBean = InjectionUtil.unwrapIfProxy(bean);
                    listenerFactory.registerEvents(realBean);
                    if (!(bean instanceof Listener)) {
                        dynamicListeners.add(new Listener() {});
                    }
                });
        long count = registry
                .getBeanDefinitions()
                .stream()
                .mapToLong(def -> def.getListenMethods().size())
                .sum();
        log.info("Registered {} listeners", events.size() + count);
    }
}