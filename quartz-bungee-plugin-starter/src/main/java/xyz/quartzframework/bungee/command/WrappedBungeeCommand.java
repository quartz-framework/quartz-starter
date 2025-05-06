package xyz.quartzframework.bungee.command;

import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import picocli.CommandLine.Model.CommandSpec;
import xyz.quartzframework.bungee.session.BungeeSession;
import xyz.quartzframework.bungee.util.CommandUtils;
import xyz.quartzframework.core.command.CommandExecutor;

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class WrappedBungeeCommand extends Command implements TabExecutor {

    private final BungeeSession session;

    private final CommandExecutor commandExecutor;

    private final CommandSpec commandSpec;

    protected WrappedBungeeCommand(CommandSpec commandSpec, BungeeSession context, CommandExecutor commandExecutor) {
        super(commandSpec.name());
        this.commandSpec = commandSpec;
        this.session = context;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        session.runWithSender(sender, () -> {
            val command = prepend(args, getName());
            val result = commandExecutor.execute(command);
            result.getOutput().forEach(s -> {
                val text = new TextComponent();
                text.setText(s);
                sender.sendMessage(text);
            });
            return result.isExists();
        });
    }

    @Override
    public String[] getAliases() {
        return commandSpec.aliases();
    }

    @SuppressWarnings("unchecked")
    private static <T> T[] prepend(T[] oldArray, T item) {
        val newArray = (T[]) Array.newInstance(oldArray.getClass().getComponentType(), oldArray.length + 1);
        System.arraycopy(oldArray, 0, newArray, 1, oldArray.length);
        newArray[0] = item;
        return newArray;
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) return Collections.emptyList();
        return session.runWithSender(sender, () -> {
            Stream<String> possibleSubcommands = CommandUtils.getPossibleSubcommands(commandSpec, args);
            Stream<String> possibleArguments = CommandUtils.getPossibleArguments(commandSpec, args);
            return Stream.concat(possibleSubcommands, possibleArguments).collect(Collectors.toList());
        });
    }
}