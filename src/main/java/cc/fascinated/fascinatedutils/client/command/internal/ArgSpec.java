package cc.fascinated.fascinatedutils.client.command.internal;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public interface ArgSpec<T> {
    /**
     * Brigadier argument type; may differ from {@code T} (e.g. {@link String} parsed into an enum).
     */
    ArgumentType<?> argumentType();

    T read(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException;

    default SuggestionProvider<FabricClientCommandSource> suggestions() {
        return null;
    }
}
