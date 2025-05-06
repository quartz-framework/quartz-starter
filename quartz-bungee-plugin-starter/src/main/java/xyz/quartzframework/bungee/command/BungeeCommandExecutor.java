package xyz.quartzframework.bungee.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.md_5.bungee.api.ChatColor;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import xyz.quartzframework.core.annotation.NoProxy;
import xyz.quartzframework.core.annotation.Property;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.command.CommandExecutor;
import xyz.quartzframework.core.command.CommandResult;
import xyz.quartzframework.core.command.picocli.CommandLineDefinition;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.BooleanUtils.toBoolean;

@Slf4j
@NoProxy
@RequiredArgsConstructor
public class BungeeCommandExecutor implements CommandExecutor {

    private final PluginBeanFactory pluginBeanFactory;

    private final CommandLineDefinition commandLineDefinition;

    @Property("${bungee.messages.command_error:&c%s}")
    private String commandErrorMessage;

    @Property("${bungee.messages.missing_parameter_error:&cMissing parameter: %s}")
    private String missingParameterErrorMessage;

    @Property("${bungee.messages.parameter_error:&cInvalid parameter: %s}")
    private String parameterErrorMessage;

    @Property("${bungee.commands.enable_cache:false}")
    private boolean cacheEnabled;

    private CommandLine commandLineCache;

    @Override
    public CommandResult execute(String... commandParts) {
        if (commandParts.length == 0) {
            return CommandResult.unknown();
        }
        try {
            if (!toBoolean(cacheEnabled) || commandLineCache == null) {
                commandLineCache = commandLineDefinition.build(pluginBeanFactory);
            }
            val output = new ArrayList<String>();
            val commands = commandLineCache.parseArgs(commandParts).asCommandLineList();
            if (commands.isEmpty()) {
                return CommandResult.unknown();
            }
            val commandLine = commands.get(commands.size() - 1);
            val command = commandLine.getCommand();
            if (command instanceof Runnable) {
                ((Runnable) command).run();
            } else if (command instanceof Callable) {
                val result = ((Callable<?>) command).call();
                output.addAll(buildOutput(result));
            }
            return new CommandResult(output);
        } catch (CommandLine.InitializationException ex) {
            log.error("Unexpected exception during command initialization", ex);
            return CommandResult.unknown();
        } catch (CommandLine.UnmatchedArgumentException ex) {
            val commandObject = ex.getCommandLine().getCommandSpec().userObject();
            if (commandObject == null){
                return CommandResult.unknown();
            }
            val message = String.format(parameterErrorMessage, String.join(", ", ex.getUnmatched()));
            return new CommandResult(ChatColor.translateAlternateColorCodes('&', message), true);
        } catch (CommandLine.MissingParameterException ex) {
            val message = String.format(missingParameterErrorMessage, ex.getMissing().get(0).paramLabel());
            return new CommandResult(ChatColor.translateAlternateColorCodes('&', message), true);
        } catch (CommandLine.ParameterException ex) {
            val message = String.format(parameterErrorMessage, ex.getArgSpec().paramLabel());
            return new CommandResult(ChatColor.translateAlternateColorCodes('&', message), true);
        } catch (Throwable ex) {
            log.error("Unexpected exception while running /{}", StringUtils.join(commandParts, " "), ex);
            return new CommandResult(ChatColor.translateAlternateColorCodes('&', commandErrorMessage.formatted(ex.getMessage())), true);
        }
    }

    private List<String> buildOutput(Object result) {
        if (result instanceof String) {
            return Collections.singletonList(ChatColor.translateAlternateColorCodes('&', (String) result));
        } else if (result instanceof Collection) {
            return ((Collection<?>) result)
                    .stream()
                    .flatMap(res -> buildOutput(res).stream())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}