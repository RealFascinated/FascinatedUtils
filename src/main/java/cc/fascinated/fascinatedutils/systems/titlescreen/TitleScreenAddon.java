package cc.fascinated.fascinatedutils.systems.titlescreen;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.SelfUser;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.InputEvent;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.screens.HUDEditorScreen;
import cc.fascinated.fascinatedutils.gui.screens.SocialScreen;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidgetHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

public class TitleScreenAddon {

    public static final TitleScreenAddon INSTANCE = new TitleScreenAddon();

    private static final float BUTTON_SIZE = 16f;
    private static final float BUTTON_GAP = 4f;
    private static final float PANEL_PADDING = 5f;
    private static final float PANEL_MARGIN_RIGHT = 8f;
    private static final float PANEL_WIDTH = PANEL_PADDING + BUTTON_SIZE + PANEL_PADDING;
    private static final float PANEL_HEIGHT = PANEL_PADDING + BUTTON_SIZE + BUTTON_GAP + BUTTON_SIZE + PANEL_PADDING;

    private static final int COLOR_PANEL_BG = 0xB0101010;
    private static final int COLOR_HOVER = 0x30FFFFFF;
    private static final int COLOR_ICON = 0xFFFFFFFF;

    private final FWidgetHost host = buildHost();

    private static FWidgetHost buildHost() {
        FWidgetHost host = new FWidgetHost();
        host.setRoot(new FWidget() {
            final FIconButtonWidget settingsBtn = new FIconButtonWidget(BUTTON_SIZE, 0f, 3f, ModUiTextures.SETTINGS::getId, true) {
                @Override
                protected int resolveButtonFillArgb(boolean hovered) {
                    return hovered ? COLOR_HOVER : 0;
                }

                @Override
                protected int resolveButtonBorderArgb(boolean hovered) {
                    return 0;
                }

                @Override
                protected int resolveContentTintArgb(boolean hovered) {
                    return COLOR_ICON;
                }
            };
            {
                settingsBtn.setDrawBorder(false);
                settingsBtn.setOnClick(() -> Minecraft.getInstance().setScreen(new HUDEditorScreen()));
                addChild(settingsBtn);
            }

            final FIconButtonWidget socialBtn = new FIconButtonWidget(BUTTON_SIZE, 0f, 3f, ModUiTextures.GROUP::getId, true) {
                @Override
                protected int resolveButtonFillArgb(boolean hovered) {
                    return hovered ? COLOR_HOVER : 0;
                }

                @Override
                protected int resolveButtonBorderArgb(boolean hovered) {
                    return 0;
                }

                @Override
                protected int resolveContentTintArgb(boolean hovered) {
                    return COLOR_ICON;
                }
            };
            {
                socialBtn.setDrawBorder(false);
                socialBtn.setOnClick(() -> Minecraft.getInstance().setScreen(new SocialScreen()));
                addChild(socialBtn);
            }

            @Override
            public void layout(UIRenderer measure, float layoutX, float layoutY, float layoutW, float layoutH) {
                setBounds(layoutX, layoutY, layoutW, layoutH);
                float panelX = layoutW - PANEL_WIDTH - PANEL_MARGIN_RIGHT;
                float panelY = layoutH / 2f - PANEL_HEIGHT / 2f;
                settingsBtn.layout(measure, panelX + PANEL_PADDING, panelY + PANEL_PADDING, BUTTON_SIZE, BUTTON_SIZE);
                socialBtn.layout(measure, panelX + PANEL_PADDING, panelY + PANEL_PADDING + BUTTON_SIZE + BUTTON_GAP, BUTTON_SIZE, BUTTON_SIZE);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                float panelX = w() - PANEL_WIDTH - PANEL_MARGIN_RIGHT;
                float panelY = h() / 2f - PANEL_HEIGHT / 2f;
                graphics.fillRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 4f, COLOR_PANEL_BG, RectCornerRoundMask.ALL);
            }
        });
        return host;
    }

    public void render(GuiGraphicsExtractor graphics) {
        Minecraft mc = Minecraft.getInstance();

        SelfUser selfUser = Alumite.INSTANCE.users().selfUser();
        if (selfUser != null) {
            String name = selfUser.user().minecraftName();
            if (name != null && !name.isBlank()) {
                graphics.text(mc.font, FormattedCharSequence.forward("Alumite logged in as " + name, Style.EMPTY), 5, 5, 0xFFFFFFFF, true);
            }
        }

        float uiWidth = UIScale.uiWidth();
        float uiHeight = UIScale.uiHeight();
        GuiRenderer renderer = new GuiRenderer(graphics, FascinatedGuiTheme.INSTANCE);
        renderer.begin(uiWidth, uiHeight);
        host.layoutAndRender(renderer, 0f, 0f, uiWidth, uiHeight, UIScale.uiPointerX(), UIScale.uiPointerY(), 0f);
        renderer.end();
    }

    public boolean mouseClicked(MouseButtonEvent event) {
        float pointerX = UIScale.uiPointerX();
        float pointerY = UIScale.uiPointerY();
        boolean pressed = host.dispatchInput(new InputEvent.MousePress(pointerX, pointerY, event.button()));
        host.dispatchInput(new InputEvent.MouseRelease(pointerX, pointerY, event.button()));
        return pressed;
    }
}
