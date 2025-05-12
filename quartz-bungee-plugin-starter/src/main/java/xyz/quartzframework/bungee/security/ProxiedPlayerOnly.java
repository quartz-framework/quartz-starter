package xyz.quartzframework.bungee.security;

import org.springframework.core.annotation.AliasFor;
import xyz.quartzframework.core.exception.PermissionDeniedException;
import xyz.quartzframework.core.security.Authorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to only allow calls from players (console not allowed)
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Authorize("#root instanceof T(net.md_5.bungee.api.connection.ProxiedPlayer)")
public @interface ProxiedPlayerOnly {

    /**
     * The message to be thrown in {@link PermissionDeniedException PermissionDeniedException}
     * if the sender is not a player.
     */
    @AliasFor(annotation = Authorize.class, attribute = "message")
    String message() default "";

}