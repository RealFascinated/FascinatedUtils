package cc.fascinated.fascinatedutils.client.command.internal;

import cc.fascinated.fascinatedutils.client.command.annotation.Arg;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Parameter;

@UtilityClass
public class DefaultArgSpecResolver {

    public static ArgSpec<?> resolve(Parameter parameter, Arg argAnn) {
        Class<?> parameterType = parameter.getType();
        if (parameterType == String.class) {
            return switch (argAnn.stringKind()) {
                case WORD -> BuiltinArgSpecs.wordString();
                case STRING -> BuiltinArgSpecs.stringString();
                case GREEDY -> BuiltinArgSpecs.greedyString();
            };
        }
        if (parameterType == int.class || parameterType == Integer.class) {
            return BuiltinArgSpecs.integer();
        }
        if (parameterType == long.class || parameterType == Long.class) {
            return BuiltinArgSpecs.longArg();
        }
        if (parameterType == float.class || parameterType == Float.class) {
            return BuiltinArgSpecs.floatArg();
        }
        if (parameterType == double.class || parameterType == Double.class) {
            return BuiltinArgSpecs.doubleArg();
        }
        if (parameterType == boolean.class || parameterType == Boolean.class) {
            return BuiltinArgSpecs.bool();
        }
        if (parameterType.isEnum()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            ArgSpec<?> spec = BuiltinArgSpecs.enumArg((Class) parameterType);
            return spec;
        }
        throw new IllegalArgumentException("No default ArgSpec for parameter type " + parameterType.getName() + " on " + parameter.getDeclaringExecutable() + ".");
    }
}
