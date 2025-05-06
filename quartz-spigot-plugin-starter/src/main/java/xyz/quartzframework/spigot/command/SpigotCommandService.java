package xyz.quartzframework.spigot.command;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.SimplePluginManager;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.bean.annotation.NoProxy;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.command.CommandExecutor;
import xyz.quartzframework.core.command.CommandService;
import xyz.quartzframework.core.command.picocli.CommandLineDefinition;
import xyz.quartzframework.spigot.session.SpigotSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@NoProxy
@Injectable
@RequiredArgsConstructor
public class SpigotCommandService implements CommandService {

    private static final String DEFAULT_COMMAND_NAME = "<main class>";

    private final CommandLineDefinition commandLineDefinition;

    private final SpigotSession session;

    private final Plugin plugin;

    private final Server server;

    private final CommandExecutor commandExecutor;

    private final PluginBeanFactory pluginBeanFactory;

    @Getter
    private boolean registered;

    @PostConstruct
    public void init() {
        try {
            List<CommandSpec> commandSpecs = getCommands();
            commandSpecs.forEach(this::registerCommand);
            log.info("Successfully registered {} commands", commandSpecs.size());
            registered = true;
        } catch (Throwable t) {
            log.warn("Failed to register commands natively, falling back to event listeners", t);
        }
    }

    @PreDestroy
    void destroy() {
        try {
            List<CommandSpec> commandSpecs = getCommands();
            commandSpecs.forEach(this::unregisterCommand);
        } catch (Throwable t) {
            log.warn("Failed to unregister commands natively", t);
        }
    }

    @Override
    @SneakyThrows
    public void registerCommand(CommandSpec commandSpec) {
        val bukkitCommandMap = server.getClass().getDeclaredField("commandMap");
        bukkitCommandMap.setAccessible(true);
        val commandMap = (CommandMap) bukkitCommandMap.get(server);
        commandMap.register(plugin.getName().toLowerCase(), new WrappedBukkitCommand(commandSpec, session, commandExecutor));
    }

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public void unregisterCommand(CommandSpec commandSpec) {
        val commandName = commandSpec.name();
        val manager = (SimplePluginManager) plugin.getServer().getPluginManager();
        val commandMapField = SimplePluginManager.class.getDeclaredField("commandMap");
        commandMapField.setAccessible(true);
        val map = (CommandMap) commandMapField.get(manager);
        val knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
        knownCommandsField.setAccessible(true);
        val knownCommands = (Map<String, Command>) knownCommandsField.get(map);
        val command = knownCommands.get(commandName);
        if (command != null) {
            command.unregister(map);
            knownCommands.remove(commandName);
        }
    }

    @Override
    public List<CommandSpec> getCommands() {
        val commandLine = commandLineDefinition.build(pluginBeanFactory);
        val commandSpec = commandLine.getCommandSpec();
        if (DEFAULT_COMMAND_NAME.equals(commandSpec.name())) {
            return commandSpec.subcommands().values().stream()
                    .map(CommandLine::getCommandSpec)
                    .filter(distinctByKey(CommandSpec::name))
                    .collect(Collectors.toList());
        }
        return Collections.singletonList(commandSpec);
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}