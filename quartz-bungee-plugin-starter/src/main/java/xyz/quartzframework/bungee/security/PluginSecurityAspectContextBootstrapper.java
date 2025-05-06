package xyz.quartzframework.bungee.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.quartzframework.bungee.session.BungeeSession;
import xyz.quartzframework.bungee.session.BungeeSessionService;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenAnnotationPresent;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.security.EnableSecurity;

@Slf4j
@NoProxy
@RequiredArgsConstructor
@ContextBootstrapper
public class PluginSecurityAspectContextBootstrapper {

    @Provide
    @ActivateWhenAnnotationPresent(EnableSecurity.class)
    BungeeSecurityAspect securityAspect(BungeeSession senderSession, BungeeSessionService sessionService) {
        return new BungeeSecurityAspect(senderSession, sessionService);
    }
}