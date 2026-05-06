package cc.fascinated.fascinatedutils.mixin.titlemenu;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.SelfUser;
import cc.fascinated.fascinatedutils.client.ModBranding;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.screens.ModSettingsScreen;
import cc.fascinated.fascinatedutils.gui.screens.SocialScreen;
import cc.fascinated.fascinatedutils.renderer.MeshRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import cc.fascinated.fascinatedutils.gui.screens.HUDEditorScreen;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Unique
    private int alumite$settingsFocusId;

    private static final int BUTTON_SIZE = 16;
    private static final int BUTTON_GAP = 4;
    private static final int PANEL_PADDING = 5;
    private static final int PANEL_MARGIN_RIGHT = 8;
    private static final int PANEL_WIDTH = PANEL_PADDING + BUTTON_SIZE + PANEL_PADDING;
    private static final int PANEL_HEIGHT = PANEL_PADDING + BUTTON_SIZE + BUTTON_GAP + BUTTON_SIZE + PANEL_PADDING;

    private static final int COLOR_PANEL_BG = 0xB0101010;
    private static final int COLOR_HOVER = 0x30FFFFFF;
    private static final int COLOR_ICON = 0xFFFFFFFF;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void alumite$drawTitleMenuOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();

        SelfUser selfUser = Alumite.INSTANCE.users().selfUser();
        if (selfUser != null) {
            String name = selfUser.user().minecraftName();
            if (name != null && !name.isBlank()) {
                graphics.text(mc.font, FormattedCharSequence.forward("Alumite logged in as " + name, Style.EMPTY), 5, 5, 0xFFFFFFFF, true);
            }
        }

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int panelX = screenWidth - PANEL_WIDTH - PANEL_MARGIN_RIGHT;
        int panelY = screenHeight / 2 - PANEL_HEIGHT / 2;

        // Panel background
        MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, null, panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 4f, COLOR_PANEL_BG, COLOR_PANEL_BG, RectCornerRoundMask.ALL);

        int settingsX = panelX + PANEL_PADDING;
        int settingsY = panelY + PANEL_PADDING;
        int socialX = panelX + PANEL_PADDING;
        int socialY = settingsY + BUTTON_SIZE + BUTTON_GAP;

        // Hover highlights
        if (alumite$inButton(mouseX, mouseY, settingsX, settingsY)) {
            MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, null, settingsX, settingsY, BUTTON_SIZE, BUTTON_SIZE, 3f, COLOR_HOVER, COLOR_HOVER, RectCornerRoundMask.ALL);
        }
        if (alumite$inButton(mouseX, mouseY, socialX, socialY)) {
            MeshRenderer.INSTANCE.enqueueRoundedGradient(graphics, null, socialX, socialY, BUTTON_SIZE, BUTTON_SIZE, 3f, COLOR_HOVER, COLOR_HOVER, RectCornerRoundMask.ALL);
        }

        MeshRenderer.INSTANCE.flush(graphics);

        // Icons (drawn after flush so they layer on top)
        int iconOffset = (BUTTON_SIZE - 16) / 2;
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ModUiTextures.SETTINGS.getId(), settingsX + iconOffset, settingsY + iconOffset, 16, 16, COLOR_ICON);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, ModUiTextures.GROUP.getId(), socialX + iconOffset, socialY + iconOffset, 16, 16, COLOR_ICON);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void alumite$onTitleMenuButtonClicked(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int panelX = screenWidth - PANEL_WIDTH - PANEL_MARGIN_RIGHT;
        int panelY = screenHeight / 2 - PANEL_HEIGHT / 2;
        int settingsX = panelX + PANEL_PADDING;
        int settingsY = panelY + PANEL_PADDING;
        int socialX = panelX + PANEL_PADDING;
        int socialY = settingsY + BUTTON_SIZE + BUTTON_GAP;

        int clickX = (int) mc.mouseHandler.getScaledXPos(mc.getWindow());
        int clickY = (int) mc.mouseHandler.getScaledYPos(mc.getWindow());

        if (alumite$inButton(clickX, clickY, settingsX, settingsY)) {
            mc.setScreen(new HUDEditorScreen());
            cir.setReturnValue(true);
        } else if (alumite$inButton(clickX, clickY, socialX, socialY)) {
            mc.setScreen(new SocialScreen());
            cir.setReturnValue(true);
        }
    }

    private static boolean alumite$inButton(int x, int y, int buttonX, int buttonY) {
        return x >= buttonX && x < buttonX + BUTTON_SIZE && y >= buttonY && y < buttonY + BUTTON_SIZE;
    }
}
