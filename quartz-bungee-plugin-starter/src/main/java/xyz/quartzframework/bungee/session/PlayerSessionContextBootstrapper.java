package xyz.quartzframework.bungee.session;

import net.md_5.bungee.api.ProxyServer;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.core.session.SenderSession;

@NoProxy
@ContextBootstrapper
public class PlayerSessionContextBootstrapper {

    @Provide
    @Preferred
    @ActivateWhenBeanMissing(SenderSession.class)
    BungeeSession session(ProxyServer server) {
        return new BungeeSession(server);
    }
}
