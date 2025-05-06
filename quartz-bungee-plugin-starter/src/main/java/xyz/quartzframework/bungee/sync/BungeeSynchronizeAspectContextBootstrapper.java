package xyz.quartzframework.bungee.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.ProxyServer;
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
public class BungeeSynchronizeAspectContextBootstrapper {

    private final Scheduler scheduler;

    private final ProxyServer server;

    @Provide
    @ActivateWhenAnnotationPresent(EnableMainThreadSynchronization.class)
    BungeeSynchronizeAspect synchronizeAspect() {
        log.info("Enabling @Synchronize feature");
        return new BungeeSynchronizeAspect(scheduler, server);
    }
}