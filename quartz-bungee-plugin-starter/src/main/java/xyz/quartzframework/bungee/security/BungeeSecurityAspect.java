package xyz.quartzframework.bungee.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import xyz.quartzframework.bungee.session.BungeeSession;
import xyz.quartzframework.bungee.session.BungeeSessionService;
import xyz.quartzframework.core.exception.PermissionDeniedException;
import xyz.quartzframework.core.exception.PlayerNotFoundException;
import xyz.quartzframework.core.security.PluginAuthorize;
import xyz.quartzframework.core.util.AopAnnotationUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class BungeeSecurityAspect {

    private final BungeeSession session;

    private final BungeeSessionService sessionService;

    private final Map<String, Expression> expressionCache = new ConcurrentHashMap<>();

    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("within(@(@xyz.quartzframework.core.security.PluginAuthorize *) *) " +
            "|| execution(@(@xyz.quartzframework.core.security.PluginAuthorize *) * *(..)) " +
            "|| @within(xyz.quartzframework.core.security.PluginAuthorize)" +
            "|| execution(@xyz.quartzframework.core.security.PluginAuthorize * *(..))")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        val sender = session.getSender();
        if (sender == null) {
            throw new PlayerNotFoundException();
        }
        val method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        val senderContext = new StandardEvaluationContext(sender);
        val parameters = method.getParameters();
        IntStream.range(0, parameters.length)
                .forEach(i -> senderContext.setVariable(parameters[i].getName(), joinPoint.getArgs()[i]));
        senderContext.setVariable("session", sessionService.current());
        AopAnnotationUtils.getApplicableAnnotations(method, PluginAuthorize.class).forEach(pluginAuthorize -> {
            val expressionSource = pluginAuthorize.value();
            val expression = expressionCache.computeIfAbsent(expressionSource, parser::parseExpression);
            senderContext.setVariable("params", pluginAuthorize.params());
            if (!toBoolean(expression.getValue(senderContext, Boolean.class))) {
                val message = StringUtils.trimToNull(ChatColor.translateAlternateColorCodes('&', pluginAuthorize.message()));
                throw new PermissionDeniedException(expressionSource, message);
            }
        });
        return joinPoint.proceed();
    }
}