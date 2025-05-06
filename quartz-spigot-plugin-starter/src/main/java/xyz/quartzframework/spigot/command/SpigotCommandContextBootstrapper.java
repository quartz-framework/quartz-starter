package xyz.quartzframework.spigot.command;

import lombok.RequiredArgsConstructor;
import xyz.quartzframework.core.bean.annotation.*;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.command.CommandExecutor;
import xyz.quartzframework.core.command.picocli.CommandLineDefinition;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;
import xyz.quartzframework.spigot.session.SpigotSession;

@NoProxy
@ContextBootstrapper
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SpigotCommandContextBootstrapper {

    private final PluginBeanFactory pluginBeanFactory;

    @Provide
    @Priority(0)
    @ActivateWhenBeanMissing(CommandExecutor.class)
    CommandExecutor commandExecutor(CommandLineDefinition commandLineDefinition) {
        return new SpigotCommandExecutor(pluginBeanFactory, commandLineDefinition);
    }

    @Provide
    @Priority(1)
    @Preferred
    CommandInterceptor commandInterceptor(SpigotSession session, CommandExecutor executor, SpigotCommandService service) {
        return new CommandInterceptor(session, executor, service);
    }
}
