package cc.fascinated.fascinatedutils.client.command.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Literal path under the {@link ClientCommand} root, in order (e.g. {@code {"screenshot", "upload"}}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subcommand {
    String[] value();
}
