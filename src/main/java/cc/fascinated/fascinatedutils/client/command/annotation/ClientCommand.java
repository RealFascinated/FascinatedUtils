package cc.fascinated.fascinatedutils.client.command.annotation;

import cc.fascinated.fascinatedutils.client.command.ClientCommandBootstrap;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Root literal for a client command class. Each class defines one Brigadier root; register with
 * {@link ClientCommandBootstrap#register(Class[])}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ClientCommand {
    /**
     * Primary root literal name (no leading slash).
     */
    String value();

    /**
     * Additional root literals that redirect to the same tree (optional).
     */
    String[] aliases() default {};
}
