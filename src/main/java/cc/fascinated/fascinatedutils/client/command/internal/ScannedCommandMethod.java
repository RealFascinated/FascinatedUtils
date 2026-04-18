package cc.fascinated.fascinatedutils.client.command.internal;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Predicate;

public record ScannedCommandMethod(Method method, MethodHandle handle, List<String> literalPath, List<ArgBinding> args,
                                   Predicate<FabricClientCommandSource> requires) {}
