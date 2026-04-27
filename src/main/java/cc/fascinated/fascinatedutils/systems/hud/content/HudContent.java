package cc.fascinated.fascinatedutils.systems.hud.content;

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

    record ItemRow(List<ItemStack> leadingStacks, ItemStack stack, String text) {
        public ItemRow {
            Objects.requireNonNull(leadingStacks, "leadingStacks cannot be null");
            Objects.requireNonNull(stack, "stack cannot be null");
            Objects.requireNonNull(text, "text cannot be null");
            leadingStacks = List.copyOf(leadingStacks);
        }

        public ItemRow(ItemStack stack, String text) {
            this(List.of(), stack, text);
        }
    }
}
