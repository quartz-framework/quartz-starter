package xyz.quartzframework.spigot.command;

import lombok.val;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import picocli.CommandLine.Model.CommandSpec;
import xyz.quartzframework.core.command.CommandExecutor;
import xyz.quartzframework.spigot.session.SpigotSession;
import xyz.quartzframework.spigot.util.CommandUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class WrappedBukkitCommand extends Command {

    private final SpigotSession session;

    private final CommandExecutor commandExecutor;

    private final CommandSpec commandSpec;

    protected WrappedBukkitCommand(CommandSpec commandSpec, SpigotSession context, CommandExecutor commandExecutor) {
        super(commandSpec.name());
        this.commandSpec = commandSpec;
        this.session = context;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        return session.runWithSender(sender, () -> {
            val command = prepend(args, label);
            val result = commandExecutor.execute(command);
            result.getOutput().forEach(sender::sendMessage);
            return result.isExists();
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        if (args.length == 0) return Collections.emptyList();
        return session.runWithSender(sender, () -> {
            Stream<String> possibleSubcommands = CommandUtils.getPossibleSubcommands(commandSpec, args);
            Stream<String> possibleArguments = CommandUtils.getPossibleArguments(commandSpec, args);
            return Stream.concat(possibleSubcommands, possibleArguments).collect(Collectors.toList());
        });
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList(commandSpec.aliases());
    }

    @Override
    public String getUsage() {
        return commandSpec.commandLine().getUsageMessage();
    }

    @Override
    public String getDescription() {
        return String.join("\n", commandSpec.usageMessage().description());
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] prepend(T[] oldArray, T item) {
        val newArray = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), oldArray.length + 1);
        System.arraycopy(oldArray, 0, newArray, 1, oldArray.length);
        newArray[0] = item;
        return newArray;
    }
}