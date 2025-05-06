package xyz.quartzframework.bungee.util;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.plugin.Event;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class EventUtil {

    private static final Map<Class<? extends Event>, Method> senderGetters = new HashMap<>();

    public CommandSender getSender(Event event) {
        if (event instanceof PlayerDisconnectEvent) {
            return ((PlayerDisconnectEvent) event).getPlayer();
        }
        return getInferredSender(event);
    }

    private CommandSender getInferredSender(Event event) {
        return getSenderMethod(event.getClass())
                .map(method -> (CommandSender) getValue(event, method))
                .orElse(null);
    }

    private Optional<Method> getSenderMethod(Class<? extends Event> eventClass) {
        return Optional.ofNullable(senderGetters.computeIfAbsent(eventClass, EventUtil::findSenderMethod));
    }

    private Method findSenderMethod(Class<? extends Event> c) {
        return Arrays.stream(ReflectionUtils.getAllDeclaredMethods(c))
                .filter(method -> method.getName().startsWith("get"))
                .filter(method -> method.getParameters().length == 0)
                .filter(method -> CommandSender.class.isAssignableFrom(method.getReturnType())
                        || ProxiedPlayer.class.isAssignableFrom(method.getReturnType()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .findFirst()
                .orElse(null);
    }

    @SneakyThrows
    private Object getValue(Object instance, Method method) {
        method.setAccessible(true);
        return method.invoke(instance);
    }
}