package cc.fascinated.fascinatedutils.client.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Brigadier argument name for a handler parameter (after {@link net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Arg {
    String value();

    /**
     * For {@link String} parameters only: how to parse the string segment.
     */
    StringKind stringKind() default StringKind.WORD;

    enum StringKind {
        WORD, STRING, GREEDY
    }
}
