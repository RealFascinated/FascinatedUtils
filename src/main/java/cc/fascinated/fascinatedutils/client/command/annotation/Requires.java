package cc.fascinated.fascinatedutils.client.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Gates command execution with a built-in client predicate (combined with {@code AND} for class + method).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Requires {
    Policy value() default Policy.NONE;

    enum Policy {
        NONE,
        /**
         * Requires {@code Minecraft.getInstance().level != null}.
         */
        WORLD_LOADED,
        /**
         * Requires an integrated (singleplayer) server.
         */
        SINGLEPLAYER
    }
}
