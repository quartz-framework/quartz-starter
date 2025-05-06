package xyz.quartzframework.spigot.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Server;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenAnnotationPresent;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.scheduler.Scheduler;
import xyz.quartzframework.core.sync.EnableMainThreadSynchronization;

@Slf4j
@NoProxy
@RequiredArgsConstructor
@ContextBootstrapper
public class SynchronizeAspectContextBootstrapper {

    private final Scheduler scheduler;

    private final Server server;

    @Provide
    @ActivateWhenAnnotationPresent(EnableMainThreadSynchronization.class)
    SpigotSynchronizeAspect synchronizeAspect() {
        log.info("Enabling @Synchronize feature");
        return new SpigotSynchronizeAspect(scheduler, server);
    }
}