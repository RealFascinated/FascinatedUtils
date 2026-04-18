package cc.fascinated.fascinatedutils.client.command.internal;

import cc.fascinated.fascinatedutils.client.command.annotation.Arg;
import cc.fascinated.fascinatedutils.client.command.annotation.ArgType;
import lombok.experimental.UtilityClass;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;

@UtilityClass
public class DefaultArgSpecResolver {

    public static ArgSpec<?> resolve(Parameter parameter, Arg argAnn) {
        ArgType typeOverride = parameter.getAnnotation(ArgType.class);
        if (typeOverride != null) {
            return instantiate(typeOverride.value());
        }
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
            @SuppressWarnings({"unchecked", "rawtypes"}) Class<? extends Enum> enumClass = (Class<? extends Enum>) parameterType;
            return BuiltinArgSpecs.enumArg(enumClass);
        }
        throw new IllegalArgumentException("No default ArgSpec for parameter type " + parameterType.getName() + " on " + parameter.getDeclaringExecutable() + "; add @ArgType with a custom ArgSpec implementation.");
    }

    private static ArgSpec<?> instantiate(Class<? extends ArgSpec<?>> specClass) {
        try {
            Constructor<? extends ArgSpec<?>> ctor = specClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            return ctor.newInstance();
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new IllegalArgumentException("@ArgType " + specClass.getName() + " must be a public class with a public no-arg constructor.", reflectiveOperationException);
        }
    }
}
