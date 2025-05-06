package xyz.quartzframework.spigot.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.bukkit.Server;
import xyz.quartzframework.core.scheduler.Scheduler;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class SpigotSynchronizeAspect {

    private final Scheduler scheduler;

    private final Server server;

    @Around("within(@(@xyz.quartzframework.core.sync.Synchronize *) *) " +
            "|| execution(@(@xyz.quartzframework.core.sync.Synchronize *) * *(..)) " +
            "|| @within(xyz.quartzframework.core.sync.Synchronize)" +
            "|| execution(@xyz.quartzframework.core.sync.Synchronize * *(..))")
    public Object synchronizeCall(ProceedingJoinPoint joinPoint) throws Throwable {
        if (server.isPrimaryThread()) {
            return joinPoint.proceed();
        }
        scheduler.scheduleSyncDelayedTask(() -> {
            try {
                joinPoint.proceed();
            } catch (Throwable throwable) {
                log.error("Error in synchronous task", throwable);
            }
        }, 0);
        return null;
    }
}