package cc.fascinated.fascinatedutils.client.command.internal;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import lombok.experimental.UtilityClass;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.Locale;

@UtilityClass
public class BuiltinArgSpecs {

    public static ArgSpec<String> wordString() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return StringArgumentType.word();
            }

            @Override
            public String read(CommandContext<FabricClientCommandSource> context, String name) {
                return StringArgumentType.getString(context, name);
            }
        };
    }

    public static ArgSpec<String> stringString() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return StringArgumentType.string();
            }

            @Override
            public String read(CommandContext<FabricClientCommandSource> context, String name) {
                return StringArgumentType.getString(context, name);
            }
        };
    }

    public static ArgSpec<String> greedyString() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return StringArgumentType.greedyString();
            }

            @Override
            public String read(CommandContext<FabricClientCommandSource> context, String name) {
                return StringArgumentType.getString(context, name);
            }
        };
    }

    public static ArgSpec<Integer> integer() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return IntegerArgumentType.integer();
            }

            @Override
            public Integer read(CommandContext<FabricClientCommandSource> context, String name) {
                return IntegerArgumentType.getInteger(context, name);
            }
        };
    }

    public static ArgSpec<Integer> integer(int min, int max) {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return IntegerArgumentType.integer(min, max);
            }

            @Override
            public Integer read(CommandContext<FabricClientCommandSource> context, String name) {
                return IntegerArgumentType.getInteger(context, name);
            }
        };
    }

    public static ArgSpec<Long> longArg() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return LongArgumentType.longArg();
            }

            @Override
            public Long read(CommandContext<FabricClientCommandSource> context, String name) {
                return LongArgumentType.getLong(context, name);
            }
        };
    }

    public static ArgSpec<Float> floatArg() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return FloatArgumentType.floatArg();
            }

            @Override
            public Float read(CommandContext<FabricClientCommandSource> context, String name) {
                return FloatArgumentType.getFloat(context, name);
            }
        };
    }

    public static ArgSpec<Double> doubleArg() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return DoubleArgumentType.doubleArg();
            }

            @Override
            public Double read(CommandContext<FabricClientCommandSource> context, String name) {
                return DoubleArgumentType.getDouble(context, name);
            }
        };
    }

    public static ArgSpec<Boolean> bool() {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return BoolArgumentType.bool();
            }

            @Override
            public Boolean read(CommandContext<FabricClientCommandSource> context, String name) {
                return BoolArgumentType.getBool(context, name);
            }
        };
    }

    public static <E extends Enum<E>> ArgSpec<E> enumArg(Class<E> enumClass) {
        return new ArgSpec<>() {
            @Override
            public com.mojang.brigadier.arguments.ArgumentType<?> argumentType() {
                return StringArgumentType.word();
            }

            @Override
            public E read(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
                String raw = StringArgumentType.getString(context, name);
                try {
                    return Enum.valueOf(enumClass, raw);
                } catch (IllegalArgumentException invalidEnumCase) {
                    try {
                        return Enum.valueOf(enumClass, raw.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException invalidEnumUpperCase) {
                        throw new CommandSyntaxException(new SimpleCommandExceptionType(new LiteralMessage("Invalid enum value: " + raw)), new LiteralMessage("Invalid enum value: " + raw));
                    }
                }
            }
        };
    }
}
