package xyz.quartzframework.spigot.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenAnnotationPresent;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.security.EnableSecurity;
import xyz.quartzframework.spigot.session.SpigotSession;
import xyz.quartzframework.spigot.session.SpigotSessionService;

@Slf4j
@NoProxy
@RequiredArgsConstructor
@ContextBootstrapper
public class PluginSecurityAspectContextBootstrapper {

    @Provide
    @ActivateWhenAnnotationPresent(EnableSecurity.class)
    SpigotSecurityAspect securityAspect(SpigotSession senderSession, SpigotSessionService sessionService) {
        log.info("Enabling plugin security...");
        return new SpigotSecurityAspect(senderSession, sessionService);
    }
}