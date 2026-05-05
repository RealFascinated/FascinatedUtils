package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui.AvatarTextureCache;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.RectCornerRoundMask;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.util.function.BiConsumer;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class SocialMessageBubbleWidget {
    private static final float AVATAR_SIZE = 20f;
    private static final float AVATAR_GAP = 8f;
    private static final float VERT_PAD = 4f;
    private static final float NAME_CONTENT_GAP = 3f;
    private static final float LINE_GAP = 2f;
    private static final float MENU_BTN_SIZE = 16f;
    private static final float MENU_BTN_RIGHT_MARGIN = 2f;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    public static FWidget build(Props props, float width) {
        return new FWidget() {
            {
                if (props.editInput() != null) {
                    addChild(props.editInput());
                }
            }

            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                int capH = measure.getFontCapHeight();
                if (props.editInput() != null) {
                    float inputH = props.editInput().intrinsicHeightForColumn(measure, widthBudget - AVATAR_SIZE - AVATAR_GAP);
                    float textBlockH = capH + NAME_CONTENT_GAP + inputH + 4f + capH;
                    return Math.max(AVATAR_SIZE, textBlockH) + VERT_PAD * 2f;
                }
                int lines = Math.max(1, countLines(props.message().content()));
                float textBlockH = capH + NAME_CONTENT_GAP + lines * (capH + LINE_GAP);
                return Math.max(AVATAR_SIZE, textBlockH) + VERT_PAD * 2f;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, width, lh);
                if (props.editInput() != null) {
                    float contentX = lx + AVATAR_SIZE + AVATAR_GAP;
                    float inputW = width - AVATAR_SIZE - AVATAR_GAP;
                    float inputY = ly + VERT_PAD + measure.getFontCapHeight() + NAME_CONTENT_GAP;
                    float inputH = props.editInput().intrinsicHeightForColumn(measure, inputW);
                    props.editInput().layout(measure, contentX, inputY, inputW, inputH);
                }
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return props.onContextMenu() != null ? PointerHitKind.TARGET : PointerHitKind.NONE;
            }

            @Override
            public UiPointerCursor pointerCursor(float pointerX, float pointerY) {
                return isMenuBtnHovered(pointerX, pointerY) ? UiPointerCursor.HAND : UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean keyDownCapture(int keyCode, int modifiers) {
                if (props.editInput() == null) {
                    return false;
                }
                if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                    props.onCancelEdit().run();
                    return true;
                }
                if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && (modifiers & GLFW.GLFW_MOD_SHIFT) == 0) {
                    props.onSaveEdit().run();
                    return true;
                }
                return false;
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                if (props.onContextMenu() == null) {
                    return false;
                }
                if (button == 0 && isMenuBtnHovered(pointerX, pointerY)) {
                    props.onContextMenu().accept(pointerX, pointerY);
                    return true;
                }
                if (button == 1) {
                    props.onContextMenu().accept(pointerX, pointerY);
                    return true;
                }
                return false;
            }

            private boolean isMenuBtnHovered(float px, float py) {
                float btnX = x() + w() - MENU_BTN_SIZE - MENU_BTN_RIGHT_MARGIN;
                float btnY = y() + VERT_PAD;
                return px >= btnX && px < btnX + MENU_BTN_SIZE && py >= btnY && py < btnY + MENU_BTN_SIZE;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                if (containsPoint(frame.pointerX(), frame.pointerY())) {
                    graphics.drawRect(x(), y(), w(), h(), 0x14FFFFFF);
                }

                ChannelMessage message = props.message();
                User author = Alumite.INSTANCE.users().cachedUser(message.authorId());
                String minecraftUuid = author != null ? author.minecraftUuid() : null;
                String displayName = author != null && author.minecraftName() != null
                        ? author.minecraftName()
                        : "#" + message.authorId();

                float avatarY = y() + VERT_PAD;
                if (minecraftUuid != null && !minecraftUuid.isBlank()) {
                    Identifier avatarTexture = AvatarTextureCache.INSTANCE.get(minecraftUuid, () -> {});
                    if (avatarTexture != null) {
                        graphics.fillRoundedRect(x(), avatarY, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF000000, RectCornerRoundMask.ALL);
                        graphics.drawTexture(avatarTexture, x(), avatarY, AVATAR_SIZE, AVATAR_SIZE, 0xFFFFFFFF);
                    } else {
                        drawInitialBadge(graphics, x(), avatarY, displayName);
                    }
                } else {
                    drawInitialBadge(graphics, x(), avatarY, displayName);
                }

                float contentX = x() + AVATAR_SIZE + AVATAR_GAP;
                float nameY = y() + VERT_PAD;
                int nameColor = props.ownMessage()
                        ? FascinatedGuiTheme.INSTANCE.textAccent()
                        : FascinatedGuiTheme.INSTANCE.textPrimary();
                graphics.drawText(displayName, contentX, nameY, nameColor, true, false);
                String timestamp = formatTimestamp(message.createdAt());
                if (!timestamp.isBlank()) {
                    float nameW = graphics.measureTextWidth(displayName, true);
                    graphics.drawText(timestamp, contentX + nameW + 6f, nameY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                }

                if (props.editInput() != null) {
                    float hintY = props.editInput().y() + props.editInput().h() + 4f;
                    graphics.drawText(Component.translatable("fascinatedutils.social.edit_message.hint").getString(), contentX, hintY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
                } else {
                    float textY = nameY + graphics.getFontCapHeight() + NAME_CONTENT_GAP;
                    String displayContent = message.content();
                    if (message.editedAt() != null && !message.editedAt().isBlank()) {
                        displayContent += " (edited)";
                    }
                    for (String line : splitLines(displayContent)) {
                        graphics.drawText(line, contentX, textY, 0xFFFFFFFF, false, false);
                        textY += graphics.getFontCapHeight() + LINE_GAP;
                    }
                }

                if (props.onContextMenu() != null && containsPoint(frame.pointerX(), frame.pointerY())) {
                    float btnX = x() + w() - MENU_BTN_SIZE - MENU_BTN_RIGHT_MARGIN;
                    float btnY = y() + VERT_PAD;
                    boolean menuBtnHovered = isMenuBtnHovered(frame.pointerX(), frame.pointerY());
                    int btnBg = menuBtnHovered ? 0x55FFFFFF : 0x33FFFFFF;
                    graphics.fillRoundedRect(btnX, btnY, MENU_BTN_SIZE, MENU_BTN_SIZE, 3f, btnBg, RectCornerRoundMask.ALL);
                    graphics.drawCenteredText("\u22EE", btnX + MENU_BTN_SIZE / 2f, btnY + (MENU_BTN_SIZE - graphics.getFontCapHeight()) / 2f, 0xFFCCCCCC, false, false);
                }
            }

            private void drawInitialBadge(GuiRenderer graphics, float bx, float by, String name) {
                String initial = name.isBlank() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
                graphics.fillRoundedRect(bx, by, AVATAR_SIZE, AVATAR_SIZE, 4f, 0xFF3B445A, RectCornerRoundMask.ALL);
                graphics.drawCenteredText(initial, bx + AVATAR_SIZE / 2f, by + (AVATAR_SIZE - graphics.getFontCapHeight()) / 2f, 0xFFFFFFFF, false, true);
            }
        };
    }

    private static String formatTimestamp(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }
        try {
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime messageTime = Instant.parse(time).atZone(zoneId);
            LocalDate messageDate = messageTime.toLocalDate();
            LocalDate today = LocalDate.now(zoneId);
            String formattedTime = TIME_FORMATTER.format(messageTime);
            if (messageDate.equals(today)) {
                return Component.translatable("fascinatedutils.social.message_time.today_at", formattedTime).getString();
            }
            if (messageDate.equals(today.minusDays(1))) {
                return Component.translatable("fascinatedutils.social.message_time.yesterday_at", formattedTime).getString();
            }
            return Component.translatable("fascinatedutils.social.message_time.full", DATE_FORMATTER.format(messageTime), formattedTime).getString();
        } catch (DateTimeParseException ignored) {
            return "";
        }
    }

    private static int countLines(String value) {
        return splitLines(value).length;
    }

    private static String[] splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return new String[]{""};
        }
        return value.split("\\R", -1);
    }

    public record Props(ChannelMessage message, boolean ownMessage, BiConsumer<Float, Float> onContextMenu,
                         FOutlinedTextInputWidget editInput, Runnable onSaveEdit, Runnable onCancelEdit) {
        public Props(ChannelMessage message, boolean ownMessage, BiConsumer<Float, Float> onContextMenu) {
            this(message, ownMessage, onContextMenu, null, null, null);
        }
    }
}
