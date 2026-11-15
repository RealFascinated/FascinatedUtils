package cc.fascinated.fascinatedutils.client.command.internal;

import cc.fascinated.fascinatedutils.client.command.annotation.Arg;
import cc.fascinated.fascinatedutils.client.command.annotation.ClientCommand;
import cc.fascinated.fascinatedutils.client.command.annotation.Subcommand;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class ClientCommandScanner {

    public static ScannedCommandClass scan(Class<?> clazz) {
        ClientCommand root = clazz.getAnnotation(ClientCommand.class);
        if (root == null) {
            throw new IllegalArgumentException("Missing @ClientCommand on " + clazz.getName());
        }
        List<ScannedCommandMethod> methods = new ArrayList<>();
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        for (Method method : clazz.getDeclaredMethods()) {
            Subcommand sub = method.getAnnotation(Subcommand.class);
            if (sub == null) {
                continue;
            }
            if (!Modifier.isPublic(method.getModifiers()) || !Modifier.isStatic(method.getModifiers())) {
                throw new IllegalStateException("@Subcommand method must be public static: " + method);
            }
            if (method.getReturnType() != int.class) {
                throw new IllegalStateException("@Subcommand method must return int: " + method);
            }
            Parameter[] params = method.getParameters();
            if (params.length == 0 || params[0].getType() != FabricClientCommandSource.class) {
                throw new IllegalStateException("@Subcommand method first parameter must be FabricClientCommandSource: " + method);
            }
            String[] path = sub.value();
            if (path.length == 0) {
                throw new IllegalStateException("@Subcommand path must not be empty: " + method);
            }
            List<ArgBinding> bindings = new ArrayList<>();
            for (int argumentParameterIndex = 1; argumentParameterIndex < params.length; argumentParameterIndex++) {
                Parameter parameter = params[argumentParameterIndex];
                Arg argAnn = parameter.getAnnotation(Arg.class);
                if (argAnn == null) {
                    throw new IllegalStateException("Parameter after FabricClientCommandSource must have @Arg: " + parameter + " in " + method);
                }
                ArgSpec<?> spec = DefaultArgSpecResolver.resolve(parameter, argAnn);
                bindings.add(new ArgBinding(argAnn.value(), spec));
            }
            Class<?>[] argTypes = new Class<?>[params.length];
            for (int parameterSlotIndex = 0; parameterSlotIndex < params.length; parameterSlotIndex++) {
                argTypes[parameterSlotIndex] = params[parameterSlotIndex].getType();
            }
            MethodType methodType = MethodType.methodType(int.class, argTypes);
            MethodHandle handle;
            try {
                handle = lookup.findStatic(clazz, method.getName(), methodType);
            } catch (NoSuchMethodException | IllegalAccessException exception) {
                throw new IllegalStateException("Cannot create MethodHandle for " + method, exception);
            }
            methods.add(new ScannedCommandMethod(method, handle, List.of(path), bindings));
        }
        if (methods.isEmpty()) {
            throw new IllegalStateException("No @Subcommand methods on " + clazz.getName());
        }
        return new ScannedCommandClass(clazz, root.value(), root.aliases(), methods);
    }

    public record ScannedCommandClass(Class<?> clazz, String rootLiteral, String[] aliases,
                                      List<ScannedCommandMethod> methods) {}
}
