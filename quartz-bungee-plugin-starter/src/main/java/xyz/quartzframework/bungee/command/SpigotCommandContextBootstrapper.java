package xyz.quartzframework.bungee.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.quartzframework.core.bean.annotation.Inject;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.annotation.Priority;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.command.CommandExecutor;
import xyz.quartzframework.core.command.picocli.CommandLineDefinition;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.ContextBootstrapper;

@Slf4j
@NoProxy
@ContextBootstrapper
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class SpigotCommandContextBootstrapper {

    private final PluginBeanFactory pluginBeanFactory;

    @Provide
    @Priority(0)
    @ActivateWhenBeanMissing(CommandExecutor.class)
    CommandExecutor commandExecutor(CommandLineDefinition commandLineDefinition) {
        return new BungeeCommandExecutor(pluginBeanFactory, commandLineDefinition);
    }
}
