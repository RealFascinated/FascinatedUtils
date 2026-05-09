package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UIScale;
import cc.fascinated.fascinatedutils.oldgui.core.TextLineLayout;
import cc.fascinated.fascinatedutils.gui2.node.TextInputNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerAvatarNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.net.URI;
import java.util.function.BiConsumer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * A single chat message row consisting of an avatar, author name with timestamp, and message content.
 *
 * <p>Messages from the local player are indented from the right to visually distinguish them.
 * Height is derived from the number of text lines in the content.
 */
public class ChatMessageNode extends PositionedNode {

    private static final int AVATAR_SIZE = 20;
    private static final int AVATAR_GAP = 8;
    private static final int PAD_V = 4;
    private static final int NAME_CONTENT_GAP = 3;
    private static final int LINE_GAP = 2;
    private static final int ATTACHMENT_GAP = 4;
    private static final int ATTACHMENT_CORNER_RADIUS = 4;
    private static final int MAX_ATTACHMENT_DISPLAY_HEIGHT = 200;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private static final int EDIT_INPUT_HEIGHT = 26;
    private static final int EDIT_INPUT_GAP = 2;

    private final Message message;
    private final PlayerAvatarNode avatar;
    private TextInputNode editInput;
    private BiConsumer<Float, Float> onContextMenu;
    private BiConsumer<Float, Float> onAuthorClick;
    private boolean hovered;
    private int[][] lastAttachmentRects = new int[0][];
    private String[] lastAttachmentUrls = new String[0];
    private int lastContentX;
    private int lastContentY;
    private int lastNameRight;

    public ChatMessageNode(Message message) {
        this.message = message;
        fullWidth();

        avatar = new PlayerAvatarNode(AVATAR_SIZE,
                () -> {
                    User author = resolveAuthor();
                    return author != null ? author.minecraftUuid() : null;
                },
                () -> {
                    User author = resolveAuthor();
                    return author != null ? author.minecraftName() : "#" + message.authorId();
                },
                () -> UserStatus.OFFLINE.color());
        avatar.setShowStatusDot(false);
        addChild(avatar);
    }

    public ChatMessageNode setEditInput(TextInputNode editInput) {
        if (this.editInput != null) {
            removeChild(this.editInput);
        }
        this.editInput = editInput;
        if (editInput != null) {
            addChild(editInput);
        }
        return this;
    }

    public ChatMessageNode setOnContextMenu(BiConsumer<Float, Float> onContextMenu) {
        this.onContextMenu = onContextMenu;
        return this;
    }

    public ChatMessageNode setOnAuthorClick(BiConsumer<Float, Float> onAuthorClick) {
        this.onAuthorClick = onAuthorClick;
        return this;
    }

    @Override
    public boolean onPointerEnter(float pointerX, float pointerY) {
        hovered = true;
        return false;
    }

    @Override
    public boolean onPointerLeave(float pointerX, float pointerY) {
        hovered = false;
        return false;
    }

    @Override
    public boolean blocksHitWhenEmpty() {
        return true;
    }

    @Override
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button == 1 && pointerX >= lastContentX) {
            boolean onAuthorName = pointerY < lastContentY && pointerX <= lastNameRight;
            if (onAuthorName) {
                if (onAuthorClick != null) {
                    onAuthorClick.accept(pointerX, pointerY);
                    return true;
                }
            } else if (onContextMenu != null) {
                onContextMenu.accept(pointerX, pointerY);
                return true;
            }
        }
        if (button == 0) {
            for (int attachmentIndex = 0; attachmentIndex < lastAttachmentRects.length; attachmentIndex++) {
                int[] rect = lastAttachmentRects[attachmentIndex];
                if (pointerX >= rect[0] && pointerX < rect[0] + rect[2]
                        && pointerY >= rect[1] && pointerY < rect[1] + rect[3]) {
                    String url = lastAttachmentUrls[attachmentIndex];
                    try {
                        Util.getPlatform().openUri(URI.create(url));
                    } catch (IllegalArgumentException ignored) {
                    }
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private User resolveAuthor() {
        return Alumite.INSTANCE != null ? Alumite.INSTANCE.users().getUser(message.authorId()) : null;
    }

    private String resolveAuthorName() {
        User author = resolveAuthor();
        return author != null && author.minecraftName() != null ? author.minecraftName() : "#" + message.authorId();
    }

    private String formatTimestamp() {
        try {
            Instant instant = Instant.parse(message.createdAt());
            ZonedDateTime messageTime = instant.atZone(ZoneId.systemDefault());
            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
            if (messageTime.toLocalDate().equals(LocalDate.now(ZoneId.systemDefault()))) {
                return TIME_FORMATTER.format(messageTime);
            }
            if (messageTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
                return "Yesterday " + TIME_FORMATTER.format(messageTime);
            }
            return DATE_FORMATTER.format(messageTime) + " " + TIME_FORMATTER.format(messageTime);
        } catch (DateTimeParseException ignored) {
            return "";
        }
    }

    @Override
    public void layout(RenderFrame renderFrame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int lineHeight = renderFrame.fontHeight();
        int nameH = lineHeight + NAME_CONTENT_GAP;
        int availableWidth = Math.max(0, parentWidth - 4 - AVATAR_SIZE - AVATAR_GAP);
        int textX = parentX + 4 + AVATAR_SIZE + AVATAR_GAP;
        int totalH;
        if (editInput != null) {
            totalH = PAD_V + nameH + EDIT_INPUT_GAP + EDIT_INPUT_HEIGHT + PAD_V;
            bounds().set(parentX, parentY, parentWidth, totalH);

            int editInputY = parentY + PAD_V + nameH + EDIT_INPUT_GAP;
            editInput.layout(renderFrame, textX, editInputY, availableWidth, EDIT_INPUT_HEIGHT);
        } else {
            int contentLines = wrappedLineCount(renderFrame, message.content(), availableWidth);
            int contentHeight = contentLines * lineHeight;
            if (contentLines > 1) {
                contentHeight += (contentLines - 1) * LINE_GAP;
            }
            totalH = PAD_V + nameH + contentHeight;
            List<AttachmentDTO> attachments = message.attachments();
            if (attachments != null) {
                for (AttachmentDTO attachment : attachments) {
                    if (!isImageAttachment(attachment)) {
                        continue;
                    }
                    totalH += ATTACHMENT_GAP;
                    totalH += attachmentDisplaySize(attachment, availableWidth)[1];
                }
            }
            totalH += PAD_V;
            bounds().set(parentX, parentY, parentWidth, totalH);
        }

        int avatarX = parentX + 4;
        int avatarY = parentY + PAD_V;
        avatar.layout(renderFrame, avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE);

        lastContentX = parentX + 4 + AVATAR_SIZE + AVATAR_GAP;
        lastContentY = parentY + PAD_V + nameH;
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        if (hovered || editInput != null) {
            renderFrame.drawRect(bounds().positionX(), bounds().positionY(), bounds().width(), bounds().height(), renderFrame.theme().rowHoverFill());
        }

        int posX = bounds().positionX();
        int posY = bounds().positionY();

        int textX = posX + 4 + AVATAR_SIZE + AVATAR_GAP;
        int lineHeight = renderFrame.fontHeight();
        int availableWidth = Math.max(0, bounds().width() - 4 - AVATAR_SIZE - AVATAR_GAP);

        String authorName = resolveAuthorName();
        String timestamp = formatTimestamp();
        int nameColor = renderFrame.theme().textPrimary();
        int mutedColor = renderFrame.theme().textMuted();
        int contentColor = renderFrame.theme().textPrimary();

        int nameY = posY + PAD_V;
        int nameWidth = renderFrame.measureTextWidth(authorName, true);
        lastNameRight = textX + nameWidth;
        renderFrame.drawText(authorName, textX, nameY, nameColor, false, true);
        if (!timestamp.isBlank()) {
            renderFrame.drawText(timestamp, textX + nameWidth + 6, nameY + 1, mutedColor, false, false);
        }

        int contentY = nameY + lineHeight + NAME_CONTENT_GAP;
        if (editInput != null) {
            // edit input is rendered as a child node — skip content text and attachments
            return;
        }
        String content = message.content() != null ? message.content() : "";
        if (!content.isBlank()) {
            for (String paragraph : splitLines(content)) {
                List<String> wrappedLines = TextLineLayout.wrapLines(paragraph, availableWidth, segment -> renderFrame.measureTextWidth(segment, false));
                if (wrappedLines.isEmpty()) {
                    contentY += lineHeight + LINE_GAP;
                } else {
                    for (String wrappedLine : wrappedLines) {
                        renderFrame.drawText(wrappedLine, textX, contentY, contentColor, false, false);
                        contentY += lineHeight + LINE_GAP;
                    }
                }
            }
        }

        List<AttachmentDTO> attachments = message.attachments();
        int imageCount = 0;
        if (attachments != null) {
            for (AttachmentDTO attachment : attachments) {
                if (isImageAttachment(attachment)) {
                    imageCount++;
                }
            }
        }

        int[][] attachmentRects = new int[imageCount][4];
        String[] attachmentUrls = new String[imageCount];
        int attachmentIndex = 0;
        if (attachments != null) {
            for (AttachmentDTO attachment : attachments) {
                if (!isImageAttachment(attachment)) {
                    continue;
                }

                contentY += ATTACHMENT_GAP;
                int[] size = attachmentDisplaySize(attachment, availableWidth);
                int attachmentWidth = size[0];
                int attachmentHeight = size[1];

                attachmentRects[attachmentIndex][0] = textX;
                attachmentRects[attachmentIndex][1] = contentY;
                attachmentRects[attachmentIndex][2] = attachmentWidth;
                attachmentRects[attachmentIndex][3] = attachmentHeight;
                attachmentUrls[attachmentIndex] = attachment.url();

                Identifier texture = UrlTextureCache.INSTANCE.get(attachment.id(), attachment.url(), null);
                if (texture != null) {
                    renderFrame.drawRoundedTexture(texture, textX, contentY, attachmentWidth, attachmentHeight, ATTACHMENT_CORNER_RADIUS, renderFrame.theme().attachmentTint());
                } else {
                    renderFrame.drawRoundedRect(textX, contentY, attachmentWidth, attachmentHeight, ATTACHMENT_CORNER_RADIUS, renderFrame.theme().attachmentPlaceholderFill());
                }

                contentY += attachmentHeight;
                attachmentIndex++;
            }
        }

        lastAttachmentRects = attachmentRects;
        lastAttachmentUrls = attachmentUrls;
    }

    private static boolean isImageAttachment(AttachmentDTO attachment) {
        return attachment != null
                && attachment.url() != null
                && attachment.id() != null
                && attachment.mimeType() != null
                && attachment.mimeType().startsWith("image/");
    }

    private static String[] splitLines(String value) {
        if (value == null || value.isEmpty()) {
            return new String[0];
        }
        return value.split("\\R", -1);
    }

    private static int wrappedLineCount(RenderFrame renderFrame, String content, int availableWidth) {
        String[] paragraphs = splitLines(content);
        if (paragraphs.length == 0) {
            return 0;
        }
        int total = 0;
        for (String paragraph : paragraphs) {
            if (paragraph.isBlank()) {
                total++;
            } else {
                List<String> wrapped = TextLineLayout.wrapLines(paragraph, availableWidth, segment -> renderFrame.measureTextWidth(segment, false));
                total += Math.max(1, wrapped.size());
            }
        }
        return total;
    }

    private static int[] attachmentDisplaySize(AttachmentDTO attachment, int availableWidth) {
        float displayWidth = Math.max(1f, Math.min(availableWidth, 220f));
        float displayHeight = Math.min(MAX_ATTACHMENT_DISPLAY_HEIGHT, displayWidth * 0.75f);

        if (attachment.width() != null && attachment.height() != null && attachment.width() > 0 && attachment.height() > 0) {
            displayWidth = attachment.width() / UIScale.SCALE;
            displayHeight = attachment.height() / UIScale.SCALE;
            if (displayHeight > MAX_ATTACHMENT_DISPLAY_HEIGHT) {
                displayWidth = MAX_ATTACHMENT_DISPLAY_HEIGHT * displayWidth / displayHeight;
                displayHeight = MAX_ATTACHMENT_DISPLAY_HEIGHT;
            }
            if (displayWidth > availableWidth) {
                displayHeight = availableWidth * displayHeight / displayWidth;
                displayWidth = availableWidth;
            }
            displayWidth = Math.max(1f, displayWidth);
            displayHeight = Math.max(1f, displayHeight);
        }

        return new int[]{Math.round(displayWidth), Math.round(displayHeight)};
    }
}
