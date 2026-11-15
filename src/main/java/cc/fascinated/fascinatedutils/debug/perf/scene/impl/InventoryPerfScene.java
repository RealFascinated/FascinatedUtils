package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;
import cc.fascinated.fascinatedutils.systems.modules.impl.BlurModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.InventoryTweaksModule;
import cc.fascinated.fascinatedutils.systems.modules.impl.ItemTooltipModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * The survival inventory open over the world with items in it: scroll and click events go to the screen, the held
 * item's tooltip is built every tick and items are given and cleared so the inventory slot hooks run.
 */
public class InventoryPerfScene extends PerfScene {
    public static final String ID = "inventory";
    private static final int INPUT_EVERY_TICKS = 5;
    private static final int RESTOCK_EVERY_TICKS = 20;

    private List<?> lastTooltip;

    public InventoryPerfScene() {
        super(ID, "Inventory screen open with items, scroll and click input, tooltips built every tick");
    }

    @Override
    public void setup(PerfSceneContext context) {
        context.command("give @s diamond_sword[enchantments={sharpness:5,unbreaking:3}] 1");
        context.command("give @s stone 64");
        context.command("give @s oak_log 32");
        Minecraft minecraft = context.getMinecraft();
        context.keepScreenOpen(() -> new InventoryScreen(minecraft.player));
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        Minecraft minecraft = context.getMinecraft();
        ItemStack heldStack = minecraft.player.getInventory().getItem(0);
        lastTooltip = heldStack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
        Screen screen = minecraft.screen;
        if (screen != null && tick % INPUT_EVERY_TICKS == 0) {
            double centerX = screen.width / 2.0;
            double centerY = screen.height / 2.0;
            screen.mouseScrolled(centerX, centerY, 0.0, tick % 2 == 0 ? 1.0 : -1.0);
            MouseButtonEvent click = new MouseButtonEvent(centerX, centerY, new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_LEFT, 0));
            screen.mouseClicked(click, false);
            screen.mouseReleased(click);
        }
        if (tick % RESTOCK_EVERY_TICKS == 0) {
            context.command("clear @s oak_log");
            context.command("give @s oak_log 32");
        }
    }

    @Override
    public void teardown(PerfSceneContext context) {
        context.command("clear @s");
        lastTooltip = null;
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("inventorytweaks.AbstractContainerScreenDragMoveMixin"), PerfCoverage.mixin("inventorytweaks.InventoryItemBreakMixin"), PerfCoverage.mixin("itemtooltip.ItemStackTooltipMixin"), PerfCoverage.mixin("ContainerEventHandlerMixin"), PerfCoverage.mixin("blur.ScreenMixin"), PerfCoverage.mixin("blur.OptionsMixin"), PerfCoverage.module(InventoryTweaksModule.class), PerfCoverage.module(ItemTooltipModule.class), PerfCoverage.module(BlurModule.class));
    }
}
