package cc.fascinated.fascinatedutils.client.command;

import cc.fascinated.fascinatedutils.client.command.internal.ClientCommandScanner;
import cc.fascinated.fascinatedutils.client.command.internal.ClientCommandTreeCompiler;
import com.mojang.brigadier.CommandDispatcher;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class ClientCommandBootstrap {
    private static final List<Class<?>> COMMAND_CLASSES = new ArrayList<>();

    public static void register(Class<?>... commandClasses) {
        Collections.addAll(COMMAND_CLASSES, commandClasses);
    }

    public static void registerWithFabric(CommandDispatcher<FabricClientCommandSource> dispatcher) {
        for (Class<?> commandClass : COMMAND_CLASSES) {
            ClientCommandScanner.ScannedCommandClass spec = ClientCommandScanner.scan(commandClass);
            ClientCommandTreeCompiler.register(dispatcher, spec);
        }
    }
}
