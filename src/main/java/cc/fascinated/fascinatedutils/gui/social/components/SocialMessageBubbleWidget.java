package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.gui.UIScale;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import net.minecraft.resources.Identifier;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.core.UiPointerCursor;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FAvatarWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FIconButtonWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.net.URI;

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
    private static final float ATTACHMENT_GAP = 4f;
    private static final float MAX_ATTACHMENT_DISPLAY_HEIGHT = 200f;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    public static FWidget build(Props props, float width) {
        FAvatarWidget avatarWidget = new FAvatarWidget(AVATAR_SIZE, 4f,
                () -> {
                    User author = Alumite.INSTANCE.users().getUser(props.message().authorId());
                    return author != null ? author.minecraftUuid() : null;
                },
                () -> {
                    User author = Alumite.INSTANCE.users().getUser(props.message().authorId());
                    return author != null && author.minecraftName() != null
                            ? author.minecraftName()
                            : "#" + props.message().authorId();
                });

        FIconButtonWidget menuBtn = props.onContextMenu() != null ? new FIconButtonWidget(MENU_BTN_SIZE, 3f, 3f, ModUiTextures.MORE_VERT::getId, true) {
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
            private float[][] lastAttachmentRects = new float[0][];
            private String[] lastAttachmentUrls = new String[0];

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
                String contentForHeight = props.message().content();
                int lines = (contentForHeight == null || contentForHeight.isEmpty()) ? 0 : Math.max(1, countLines(contentForHeight));
                float textBlockH = capH + NAME_CONTENT_GAP + lines * (capH + LINE_GAP);
                float availableWidth = widthBudget - AVATAR_SIZE - AVATAR_GAP;
                for (AttachmentDTO attachment : props.message().attachments()) {
                    if (attachment.mimeType() != null && attachment.mimeType().startsWith("image/")) {
                        textBlockH += ATTACHMENT_GAP + attachmentDisplaySize(attachment, availableWidth)[1];
                    }
                }
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
                for (float[] rect : lastAttachmentRects) {
                    if (pointerX >= rect[0] && pointerX < rect[0] + rect[2] && pointerY >= rect[1] && pointerY < rect[1] + rect[3]) {
                        return UiPointerCursor.HAND;
                    }
                }
                return UiPointerCursor.DEFAULT;
            }

            @Override
            public boolean click(float pointerX, float pointerY, int button) {
                if (button == 0) {
                    for (int attachIdx = 0; attachIdx < lastAttachmentRects.length; attachIdx++) {
                        float[] rect = lastAttachmentRects[attachIdx];
                        if (pointerX >= rect[0] && pointerX < rect[0] + rect[2] && pointerY >= rect[1] && pointerY < rect[1] + rect[3]) {
                            String url = lastAttachmentUrls[attachIdx];
                            try {
                                Util.getPlatform().openUri(URI.create(url));
                            } catch (IllegalArgumentException ignored) {}
                            return true;
                        }
                    }
                }
                return false;
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

                Message message = props.message();
                User author = Alumite.INSTANCE.users().getUser(message.authorId());
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
                    if (displayContent != null && message.editedAt() != null && !message.editedAt().isBlank()) {
                        displayContent += " (edited)";
                    }
                    if (displayContent != null && !displayContent.isEmpty()) {
                        for (String line : splitLines(displayContent)) {
                            graphics.drawText(line, contentX, textY, 0xFFFFFFFF, false, false);
                            textY += graphics.getFontCapHeight() + LINE_GAP;
                        }
                    }
                    float availableWidth = w() - AVATAR_SIZE - AVATAR_GAP;
                    int imageCount = 0;
                    for (AttachmentDTO att : message.attachments()) {
                        if (att.mimeType() != null && att.mimeType().startsWith("image/")) imageCount++;
                    }
                    float[][] attachRects = new float[imageCount][4];
                    String[] attachUrls = new String[imageCount];
                    int attachIdx = 0;
                    for (AttachmentDTO attachment : message.attachments()) {
                        if (attachment.mimeType() == null || !attachment.mimeType().startsWith("image/")) {
                            continue;
                        }
                        float[] size = attachmentDisplaySize(attachment, availableWidth);
                        float attachW = size[0];
                        float attachH = size[1];
                        attachRects[attachIdx][0] = contentX;
                        attachRects[attachIdx][1] = textY;
                        attachRects[attachIdx][2] = attachW;
                        attachRects[attachIdx][3] = attachH;
                        attachUrls[attachIdx] = attachment.url();
                        attachIdx++;
                        Identifier texture = UrlTextureCache.INSTANCE.get(attachment.id(), attachment.url(), null);
                        if (texture != null) {
                            graphics.drawTexture(texture, contentX, textY, attachW, attachH, 0xFFFFFFFF);
                        } else {
                            graphics.drawRect(contentX, textY, attachW, attachH, 0x33FFFFFF);
                        }
                        textY += attachH + ATTACHMENT_GAP;
                    }
                    lastAttachmentRects = attachRects;
                    lastAttachmentUrls = attachUrls;
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

    private static float[] attachmentDisplaySize(AttachmentDTO attachment, float availableWidth) {
        if (attachment.width() != null && attachment.height() != null && attachment.width() > 0 && attachment.height() > 0) {
            float scale = UIScale.uiWidth() / Math.max(1f, Minecraft.getInstance().getWindow().getWidth());
            float dispW = attachment.width() * scale;
            float dispH = attachment.height() * scale;
            if (dispH > MAX_ATTACHMENT_DISPLAY_HEIGHT) {
                dispW = MAX_ATTACHMENT_DISPLAY_HEIGHT * attachment.width() / attachment.height();
                dispH = MAX_ATTACHMENT_DISPLAY_HEIGHT;
            }
            if (dispW > availableWidth) {
                dispH = availableWidth * dispH / dispW;
                dispW = availableWidth;
            }
            return new float[]{dispW, dispH};
        }
        return new float[]{Math.min(availableWidth, 200f), 100f};
    }

    private static String[] splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return new String[]{""};
        }
        return value.split("\\R", -1);
    }

    public record Props(Message message, boolean ownMessage, BiConsumer<Float, Float> onContextMenu,
                        FOutlinedTextInputWidget editInput, Runnable onSaveEdit, Runnable onCancelEdit) {
        public Props(Message message, boolean ownMessage, BiConsumer<Float, Float> onContextMenu) {
            this(message, ownMessage, onContextMenu, null, null, null);
        }
    }
}
