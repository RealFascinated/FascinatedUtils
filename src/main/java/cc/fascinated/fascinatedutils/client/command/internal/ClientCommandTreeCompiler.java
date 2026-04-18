package cc.fascinated.fascinatedutils.client.command.internal;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.*;

@UtilityClass
public class ClientCommandTreeCompiler {

    public static void register(com.mojang.brigadier.CommandDispatcher<FabricClientCommandSource> dispatcher, ClientCommandScanner.ScannedCommandClass spec) {
        LitNode virtualRoot = new LitNode();
        for (ScannedCommandMethod scannedMethod : spec.methods()) {
            LitNode currentNode = virtualRoot;
            for (String segment : scannedMethod.literalPath()) {
                currentNode = currentNode.next.computeIfAbsent(segment, key -> new LitNode());
            }
            currentNode.terminal.add(scannedMethod);
        }
        List<String> rootNames = new ArrayList<>();
        rootNames.add(spec.rootLiteral());
        for (String alias : spec.aliases()) {
            if (alias != null && !alias.isEmpty()) {
                rootNames.add(alias);
            }
        }
        for (String rootName : rootNames) {
            LiteralArgumentBuilder<FabricClientCommandSource> root = ClientCommands.literal(rootName).requires(spec.classRequires());
            for (Map.Entry<String, LitNode> entry : virtualRoot.next.entrySet()) {
                root.then(buildLitChain(entry.getValue(), entry.getKey(), spec.clazz()));
            }
            dispatcher.register(root);
        }
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildLitChain(LitNode node, String literal, Class<?> commandClass) {
        LiteralArgumentBuilder<FabricClientCommandSource> literalBuilder = ClientCommands.literal(literal);
        for (Map.Entry<String, LitNode> entry : node.next.entrySet()) {
            literalBuilder.then(buildLitChain(entry.getValue(), entry.getKey(), commandClass));
        }
        if (!node.terminal.isEmpty()) {
            attachTerminal(literalBuilder, node.terminal, commandClass);
        }
        return literalBuilder;
    }

    private static void attachTerminal(LiteralArgumentBuilder<FabricClientCommandSource> lit, List<ScannedCommandMethod> terminal, Class<?> commandClass) {
        List<ScannedCommandMethod> zero = terminal.stream().filter(t -> t.args().isEmpty()).toList();
        if (zero.size() > 1) {
            throw new IllegalStateException("Multiple zero-argument handlers for literal " + lit.getLiteral());
        }
        List<ScannedCommandMethod> positive = terminal.stream().filter(t -> !t.args().isEmpty()).sorted(Comparator.comparingInt(t -> t.args().size())).toList();
        for (int firstHandlerIndex = 0; firstHandlerIndex < positive.size(); firstHandlerIndex++) {
            for (int secondHandlerIndex = firstHandlerIndex + 1; secondHandlerIndex < positive.size(); secondHandlerIndex++) {
                if (!isArgPrefix(positive.get(firstHandlerIndex).args(), positive.get(secondHandlerIndex).args())) {
                    throw new IllegalStateException("Handlers at '" + lit.getLiteral() + "' are not prefix-compatible: " + positive.get(firstHandlerIndex).method().getName() + " vs " + positive.get(secondHandlerIndex).method().getName());
                }
            }
        }
        if (positive.isEmpty() && zero.isEmpty()) {
            throw new IllegalStateException("No handler for literal " + lit.getLiteral());
        }
        Command<FabricClientCommandSource> zeroExec = zero.isEmpty() ? null : ctx -> invoke(zero.get(0), commandClass, ctx);
        if (!positive.isEmpty()) {
            ScannedCommandMethod longest = positive.get(positive.size() - 1);
            List<Frame> frames = new ArrayList<>();
            for (ArgBinding argBinding : longest.args()) {
                frames.add(new Frame(argBinding));
            }
            for (ScannedCommandMethod scannedMethod : positive) {
                int handlerFrameIndex = scannedMethod.args().size() - 1;
                Frame handlerFrame = frames.get(handlerFrameIndex);
                if (handlerFrame.endMethod != null) {
                    throw new IllegalStateException("Duplicate handler arity " + scannedMethod.args().size() + " at literal " + lit.getLiteral());
                }
                handlerFrame.endMethod = scannedMethod;
            }
            if (frames.get(frames.size() - 1).endMethod != longest) {
                throw new IllegalStateException("Internal compiler error: longest handler not at leaf frame");
            }
            RequiredArgumentBuilder<FabricClientCommandSource, ?> argRoot = compileFrame(frames, 0, commandClass);
            if (zeroExec != null) {
                lit.executes(zeroExec).then(argRoot);
            }
            else {
                lit.then(argRoot);
            }
        }
        else {
            lit.executes(zeroExec);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static RequiredArgumentBuilder<FabricClientCommandSource, ?> compileFrame(List<Frame> frames, int index, Class<?> commandClass) {
        Frame frame = frames.get(index);
        RequiredArgumentBuilder<FabricClientCommandSource, ?> argumentBuilder = ClientCommands.argument(frame.binding.name(), (ArgumentType) frame.binding.spec().argumentType());
        if (frame.binding.spec().suggestions() != null) {
            argumentBuilder.suggests(frame.binding.spec().suggestions());
        }
        if (frame.endMethod != null) {
            ScannedCommandMethod terminalMethod = frame.endMethod;
            argumentBuilder.executes(commandContext -> invoke(terminalMethod, commandClass, commandContext));
        }
        if (index < frames.size() - 1) {
            argumentBuilder.then(compileFrame(frames, index + 1, commandClass));
        }
        else if (frame.endMethod == null) {
            throw new IllegalStateException("Missing handler at final argument frame");
        }
        return argumentBuilder;
    }

    private static boolean isArgPrefix(List<ArgBinding> shorter, List<ArgBinding> longer) {
        if (shorter.size() > longer.size()) {
            return false;
        }
        for (int argumentIndex = 0; argumentIndex < shorter.size(); argumentIndex++) {
            ArgBinding shorterArgument = shorter.get(argumentIndex);
            ArgBinding longerArgument = longer.get(argumentIndex);
            if (!shorterArgument.name().equals(longerArgument.name())) {
                return false;
            }
            if (!sameArgumentType(shorterArgument.spec(), longerArgument.spec())) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameArgumentType(ArgSpec<?> leftArgSpec, ArgSpec<?> rightArgSpec) {
        return leftArgSpec.argumentType().getClass() == rightArgSpec.argumentType().getClass();
    }

    @SuppressWarnings("unchecked")
    private static int invoke(ScannedCommandMethod scannedMethod, Class<?> commandClass, CommandContext<FabricClientCommandSource> commandContext) {
        try {
            if (!scannedMethod.requires().test(commandContext.getSource())) {
                commandContext.getSource().sendError(Component.literal("This command cannot be used in the current context."));
                return 0;
            }
            FabricClientCommandSource commandSource = commandContext.getSource();
            Object[] commandArguments = new Object[1 + scannedMethod.args().size()];
            commandArguments[0] = commandSource;
            for (int argumentIndex = 0; argumentIndex < scannedMethod.args().size(); argumentIndex++) {
                ArgBinding binding = scannedMethod.args().get(argumentIndex);
                commandArguments[argumentIndex + 1] = ((ArgSpec<Object>) binding.spec()).read(commandContext, binding.name());
            }
            return (int) scannedMethod.handle().invokeWithArguments(commandArguments);
        } catch (CommandSyntaxException syntaxException) {
            String message = syntaxException.getMessage();
            commandContext.getSource().sendError(Component.literal(message != null ? message : "Invalid command"));
            return 0;
        } catch (Throwable commandFailure) {
            throw new RuntimeException("Command failed: " + commandClass.getName() + "." + scannedMethod.method().getName(), commandFailure);
        }
    }

    private static class LitNode {
        private final Map<String, LitNode> next = new LinkedHashMap<>();
        private final List<ScannedCommandMethod> terminal = new ArrayList<>();
    }

    private static class Frame {
        private final ArgBinding binding;
        private ScannedCommandMethod endMethod;

        private Frame(ArgBinding binding) {
            this.binding = binding;
        }
    }
}
