package cc.fascinated.fascinatedutils.systems.modules.impl.containerpreview;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.List;

@RequiredArgsConstructor
public class ClientContainerPreviewTooltip implements ClientTooltipComponent {
    private static final Identifier SLOT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/bundle/slot_background");
    private static final int SLOT_SIZE = 24;
    private static final int COLUMNS = 9;

    private final List<ItemStack> contents;

    @Override
    public int getHeight(Font font) {
        return ((contents.size() + COLUMNS - 1) / COLUMNS) * SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return Math.min(COLUMNS, contents.size()) * SLOT_SIZE;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    @Override
    public void extractImage(Font font, int x, int y, int width, int height, GuiGraphicsExtractor graphics) {
        for (int index = 0; index < contents.size(); index++) {
            int slotX = x + (index % COLUMNS) * SLOT_SIZE;
            int slotY = y + (index / COLUMNS) * SLOT_SIZE;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_BACKGROUND_SPRITE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);
            ItemStack content = contents.get(index);
            if (!content.isEmpty()) {
                graphics.item(content, slotX + 4, slotY + 4);
                graphics.itemDecorations(font, content, slotX + 4, slotY + 4);
            }
        }
    }
}
