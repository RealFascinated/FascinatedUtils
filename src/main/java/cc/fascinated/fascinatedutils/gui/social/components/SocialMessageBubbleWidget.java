package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.BiConsumer;

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
        FAvatarWidget avatarWidget = new FAvatarWidget(AVATAR_SIZE, 4f,
                () -> {
                    User author = Alumite.INSTANCE.users().cachedUser(props.message().authorId());
                    return author != null ? author.minecraftUuid() : null;
                },
                () -> {
                    User author = Alumite.INSTANCE.users().cachedUser(props.message().authorId());
                    return author != null && author.minecraftName() != null
                            ? author.minecraftName()
                            : "#" + props.message().authorId();
                });

        FIconButtonWidget menuBtn = props.onContextMenu() != null ? new FIconButtonWidget(MENU_BTN_SIZE, 3f, () -> "\u22EE") {
            @Override
            protected int resolveButtonFillArgb(boolean hovered) {
                return hovered ? 0x55FFFFFF : 0x33FFFFFF;
            }

            @Override
            protected int resolveButtonBorderArgb(boolean hovered) {
                return resolveButtonFillArgb(hovered);
            }

            @Override
            protected int resolveContentTintArgb(boolean hovered) {
                return 0xFFCCCCCC;
            }

            @Override
            public boolean click(float pointerX, float pointerY, int button) {
                if (button == 0) {
                    props.onContextMenu().accept(pointerX, pointerY);
                    return true;
                }
                return false;
            }
        } : null;
        if (menuBtn != null) {
            menuBtn.setDrawBorder(false);
        }

        return new FWidget() {
            {
                addChild(avatarWidget);
                if (menuBtn != null) {
                    addChild(menuBtn);
                }
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
                avatarWidget.layout(measure, lx, ly + VERT_PAD, AVATAR_SIZE, AVATAR_SIZE);
                if (menuBtn != null) {
                    menuBtn.layout(measure, lx + width - MENU_BTN_SIZE - MENU_BTN_RIGHT_MARGIN, ly + VERT_PAD, MENU_BTN_SIZE, MENU_BTN_SIZE);
                }
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
                return UiPointerCursor.DEFAULT;
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
                if (button == 1) {
                    props.onContextMenu().accept(pointerX, pointerY);
                    return true;
                }
                return false;
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                boolean hovered = containsPoint(frame.pointerX(), frame.pointerY());
                if (hovered) {
                    graphics.drawRect(x(), y(), w(), h(), 0x14FFFFFF);
                }

                ChannelMessage message = props.message();
                User author = Alumite.INSTANCE.users().cachedUser(message.authorId());
                String displayName = author != null && author.minecraftName() != null
                        ? author.minecraftName()
                        : "#" + message.authorId();

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
                    graphics.drawText(Component.translatable("alumite.social.edit_message.hint").getString(), contentX, hintY, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
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

                if (menuBtn != null) {
                    menuBtn.setVisible(hovered);
                }
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
                return Component.translatable("alumite.social.message_time.today_at", formattedTime).getString();
            }
            if (messageDate.equals(today.minusDays(1))) {
                return Component.translatable("alumite.social.message_time.yesterday_at", formattedTime).getString();
            }
            return Component.translatable("alumite.social.message_time.full", DATE_FORMATTER.format(messageTime), formattedTime).getString();
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
