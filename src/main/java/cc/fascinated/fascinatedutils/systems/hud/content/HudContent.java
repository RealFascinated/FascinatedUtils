package cc.fascinated.fascinatedutils.systems.hud.content;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;

public sealed interface HudContent {

    record TextLines(List<String> miniMessageLines) implements HudContent {
        public TextLines {
            Objects.requireNonNull(miniMessageLines, "miniMessageLines cannot be null");
        }
    }

    record ItemRows(List<ItemRow> rows) implements HudContent {
        public ItemRows {
            Objects.requireNonNull(rows, "rows cannot be null");
        }
    }

    record Custom(String id) implements HudContent {
        public Custom {
            Objects.requireNonNull(id, "id cannot be null");
        }
    }

    /**
     * Identifies an item to display in an {@link ItemRow}.
     *
     * <p>{@link Real} carries an actual in-game stack from player data.
     * {@link Preview} carries only an {@link Item} reference for editor previews;
     * it builds a safe stack via {@link Holder#direct(Object, DataComponentMap)} so
     * {@link Holder.Direct#components()} returns a map we supply directly and never
     * touches {@code Holder.Reference.components()}, which is only bound once a world
     * loads.  The supplied map includes {@link DataComponents#ITEM_MODEL} so the item
     * renderer can resolve the correct model even before registry components are bound.
     */
    sealed interface HudItemSpec permits HudItemSpec.Real, HudItemSpec.Preview {

        boolean isEmpty();

        ItemStack toStack();

        record Real(ItemStack stack) implements HudItemSpec {
            @Override
            public boolean isEmpty() { return stack.isEmpty(); }
            @Override
            public ItemStack toStack() { return stack; }
        }

        record Preview(Item item) implements HudItemSpec {
            @Override
            public boolean isEmpty() { return false; }

            @Override
            public ItemStack toStack() {
                ResourceKey<Item> key = item.builtInRegistryHolder().unwrapKey().orElse(null);
                if (key == null) {
                    return ItemStack.EMPTY;
                }
                Identifier modelId = key.identifier();
                DataComponentMap baseComponents = DataComponentMap.builder()
                        .set(DataComponents.ITEM_MODEL, modelId)
                        .set(DataComponents.MAX_DAMAGE, 100)
                        .build();
                return new ItemStack(Holder.direct(item, baseComponents), 1, DataComponentPatch.EMPTY);
            }
        }
    }

    record ItemRow(List<HudItemSpec> leadingSpecs, HudItemSpec spec, String text) {
        public ItemRow {
            Objects.requireNonNull(leadingSpecs, "leadingSpecs cannot be null");
            Objects.requireNonNull(spec, "spec cannot be null");
            Objects.requireNonNull(text, "text cannot be null");
            leadingSpecs = List.copyOf(leadingSpecs);
        }

        public ItemRow(HudItemSpec spec, String text) {
            this(List.of(), spec, text);
        }
    }
}
