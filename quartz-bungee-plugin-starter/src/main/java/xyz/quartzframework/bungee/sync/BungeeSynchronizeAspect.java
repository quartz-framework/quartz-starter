package xyz.quartzframework.bungee.sync;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.md_5.bungee.api.ProxyServer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import xyz.quartzframework.core.scheduler.Scheduler;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class BungeeSynchronizeAspect {

    private final Scheduler scheduler;

    private final ProxyServer server;

    @Around("within(@(@xyz.quartzframework.core.sync.Synchronize *) *) " +
            "|| execution(@(@xyz.quartzframework.core.sync.Synchronize *) * *(..)) " +
            "|| @within(xyz.quartzframework.core.sync.Synchronize)" +
            "|| execution(@xyz.quartzframework.core.sync.Synchronize * *(..))")
    public Object synchronizeCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // TODO: not implemented yet
        return joinPoint.proceed();
    }
}