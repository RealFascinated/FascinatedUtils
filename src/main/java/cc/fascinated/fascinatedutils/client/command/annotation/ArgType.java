package cc.fascinated.fascinatedutils.client.command.annotation;

import cc.fascinated.fascinatedutils.client.command.internal.ArgSpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Override automatic {@link ArgSpec} resolution for this parameter.
 * The class must be a public, non-abstract class with a public no-arg constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface ArgType {
    Class<? extends ArgSpec<?>> value();
}
