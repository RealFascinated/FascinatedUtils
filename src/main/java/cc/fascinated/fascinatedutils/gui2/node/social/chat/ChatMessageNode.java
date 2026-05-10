package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.api.channel.json.AttachmentDTO;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.caches.UrlTextureCache;
import cc.fascinated.fascinatedutils.gui2.core.PixelSize;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UIScale;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.node.ClickableNode;
import cc.fascinated.fascinatedutils.gui2.node.ImageNode;
import cc.fascinated.fascinatedutils.gui2.node.RectNode;
import cc.fascinated.fascinatedutils.gui2.node.TextboxInputNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerAvatarNode;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.gui2.render.UiText;
import cc.fascinated.fascinatedutils.gui2.theme.UiThemeRepository;
import net.minecraft.util.Util;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class ChatMessageNode extends PositionedNode {

    private static final int AVATAR_SIZE = 20;
    private static final int AVATAR_GAP = 8;
    private static final int PAD_V = 4;
    private static final int NAME_CONTENT_GAP = 3;
    private static final int LINE_GAP = 2;
    private static final int ATTACHMENT_GAP = 4;
    private static final int ATTACHMENT_CORNER_RADIUS = 4;
    private static final int BACKGROUND_CORNER_RADIUS = 4;
    private static final int MAX_ATTACHMENT_DISPLAY_HEIGHT = 200;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

    private static final int EDIT_INPUT_GAP = 2;
    private static final int EDIT_HINT_GAP = 4;
    private static final float EDITED_SCALE = 0.8f;

    private final Message message;
    private final RectNode hoverBg;
    private final PlayerAvatarNode avatar;
    private final AuthorRowNode authorRow;
    private final ContentNode contentNode;
    private final List<ClickableNode> attachmentNodes = new ArrayList<>();
    private TextboxInputNode editInput;
    private int editHintY = -1;
    private BiConsumer<Float, Float> onContextMenu;
    private BiConsumer<Float, Float> onAuthorClick;
    private boolean hovered;

    public ChatMessageNode(Message message) {
        this.message = message;
        fullWidth();

        hoverBg = new RectNode()
                .setFillResolver(theme -> (hovered || editInput != null) ? theme.rowHoverFill() : 0)
                .setCornerRadius(BACKGROUND_CORNER_RADIUS);
        hoverBg.full();
        addChild(hoverBg);

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

        authorRow = new AuthorRowNode();
        addChild(authorRow);

        contentNode = new ContentNode();
        addChild(contentNode);

        List<AttachmentDTO> attachments = message.attachments();
        if (attachments != null) {
            for (AttachmentDTO attachment : attachments) {
                if (!isImageAttachment(attachment)) {
                    continue;
                }
                String url = attachment.url();
                ClickableNode attachmentNode = new ClickableNode()
                        .setOnPrimaryClick(() -> {
                            try {
                                Util.getPlatform().openUri(URI.create(url));
                            } catch (IllegalArgumentException ignored) {
                            }
                        });
                attachmentNode.addChild(new RectNode()
                        .setFillSupplier(() -> UrlTextureCache.INSTANCE.get(url, null) == null
                                ? UiThemeRepository.get().attachmentPlaceholderFill()
                                : 0)
                        .setCornerRadius(ATTACHMENT_CORNER_RADIUS)
                        .full());
                attachmentNode.addChild(new ImageNode()
                        .setTextureSupplier(() -> UrlTextureCache.INSTANCE.get(url, null))
                        .setCornerRadius(ATTACHMENT_CORNER_RADIUS)
                        .full());
                attachmentNodes.add(attachmentNode);
                addChild(attachmentNode);
            }
        }
    }

    public ChatMessageNode setEditInput(TextboxInputNode editInput) {
        if (this.editInput != null) {
            removeChild(this.editInput);
        }
        this.editInput = editInput;
        if (editInput != null) {
            addChild(editInput);
        }
        boolean editing = editInput != null;
        contentNode.setVisible(!editing);
        for (ClickableNode attachmentNode : attachmentNodes) {
            attachmentNode.setVisible(!editing);
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
    public boolean onClick(float pointerX, float pointerY, int button) {
        if (button == 1 && onContextMenu != null && !authorRow.contains(pointerX, pointerY)) {
            onContextMenu.accept(pointerX, pointerY);
            return true;
        }
        return false;
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
        int nameY = parentY + PAD_V;
        int totalH;

        if (editInput != null) {
            int editInputHeight = editInput.preferredHeight(renderFrame, availableWidth);
            int editInputY = nameY + nameH + EDIT_INPUT_GAP;
            editInput.layout(renderFrame, textX, editInputY, availableWidth, editInputHeight);
            editHintY = editInputY + editInputHeight + EDIT_HINT_GAP;
            totalH = PAD_V + nameH + EDIT_INPUT_GAP + editInputHeight + EDIT_HINT_GAP + renderFrame.fontHeight() + PAD_V;
        } else {
            editHintY = -1;
            UiText contentText = buildContentText(renderFrame);
            int contentHeight = contentText.wrappedHeight(renderFrame, availableWidth, LINE_GAP);
            int contentY = nameY + nameH;
            totalH = PAD_V + nameH + contentHeight;

            contentNode.layout(renderFrame, textX, contentY, availableWidth, contentHeight);

            int cursorY = contentY + contentHeight;
            int attachmentIndex = 0;
            List<AttachmentDTO> attachments = message.attachments();
            if (attachments != null) {
                for (AttachmentDTO attachment : attachments) {
                    if (!isImageAttachment(attachment)) {
                        continue;
                    }
                    cursorY += ATTACHMENT_GAP;
                    int[] size = attachmentDisplaySize(attachment, attachment.url(), availableWidth);
                    totalH += ATTACHMENT_GAP + size[1];
                    attachmentNodes.get(attachmentIndex).layout(renderFrame, textX, cursorY, size[0], size[1]);
                    cursorY += size[1];
                    attachmentIndex++;
                }
            }
            totalH += PAD_V;
        }

        bounds().set(parentX, parentY, parentWidth, totalH);
        hoverBg.layout(renderFrame, parentX, parentY, parentWidth, totalH);
        avatar.layout(renderFrame, parentX + 4, nameY, AVATAR_SIZE, AVATAR_SIZE);
        authorRow.layout(renderFrame, textX, nameY, availableWidth, lineHeight);
    }

    @Override
    protected void renderSelf(RenderFrame renderFrame, float deltaSeconds) {
        if (editInput == null || editHintY < 0) {
            return;
        }
        int hintX = bounds().positionX() + 4 + AVATAR_SIZE + AVATAR_GAP;
        UiText.of("escape to ").color(renderFrame.theme().textPrimary())
                .append(UiText.of("cancel").color(renderFrame.theme().textMuted()))
                .append(UiText.of(" \u2022 enter to ").color(renderFrame.theme().textPrimary()))
                .append(UiText.of("save").color(renderFrame.theme().textMuted()))
                .draw(renderFrame, hintX, editHintY);
    }

    private UiText buildContentText(RenderFrame frame) {
        String content = message.content() != null ? message.content() : "";
        UiText text = UiText.of(content).color(frame.theme().textPrimary());
        if (message.editedAt() != null && !message.editedAt().isBlank()) {
            text = text.append(UiText.of(" (edited)").color(frame.theme().textMuted()).scale(EDITED_SCALE));
        }
        return text;
    }

    private static boolean isImageAttachment(AttachmentDTO attachment) {
        return attachment != null
                && attachment.url() != null
                && attachment.id() != null
                && attachment.mimeType() != null
                && attachment.mimeType().startsWith("image/");
    }

    private static int[] attachmentDisplaySize(AttachmentDTO attachment, String url, int availableWidth) {
        float displayWidth = Math.max(1f, Math.min(availableWidth, 220f));
        float displayHeight = Math.min(MAX_ATTACHMENT_DISPLAY_HEIGHT, displayWidth * 0.75f);

        Integer natPixelWidth = attachment.width();
        Integer natPixelHeight = attachment.height();
        if ((natPixelWidth == null || natPixelWidth <= 0) && url != null) {
            PixelSize cached = UrlTextureCache.INSTANCE.getSizePixels(url);
            if (cached != null) {
                natPixelWidth = cached.width();
                natPixelHeight = cached.height();
            }
        }

        if (natPixelWidth != null && natPixelHeight != null && natPixelWidth > 0 && natPixelHeight > 0) {
            displayWidth = natPixelWidth / UIScale.SCALE;
            displayHeight = natPixelHeight / UIScale.SCALE;
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

    private class AuthorRowNode extends UiNode {

        @Override
        public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
            bounds().set(positionX, positionY, width, height);
        }

        @Override
        protected void renderSelf(RenderFrame frame, float deltaSeconds) {
            String authorName = resolveAuthorName();
            String timestamp = formatTimestamp();
            int posX = bounds().positionX();
            int posY = bounds().positionY();
            UiText authorText = UiText.of(authorName).color(frame.theme().textPrimary());
            int nameWidth = authorText.width(frame);
            authorText.draw(frame, posX, posY);
            if (!timestamp.isBlank()) {
                UiText.of(timestamp).color(frame.theme().textMuted()).draw(frame, posX + nameWidth + 6, posY + 1);
            }
        }

        @Override
        public boolean blocksHitWhenEmpty() {
            return true;
        }

        @Override
        public boolean onClick(float pointerX, float pointerY, int button) {
            if (button == 1 && onAuthorClick != null) {
                onAuthorClick.accept(pointerX, pointerY);
                return true;
            }
            return false;
        }
    }

    private class ContentNode extends UiNode {

        @Override
        public void layout(RenderFrame renderFrame, int positionX, int positionY, int width, int height) {
            bounds().set(positionX, positionY, width, height);
        }

        @Override
        protected void renderSelf(RenderFrame frame, float deltaSeconds) {
            buildContentText(frame).draw(frame, bounds().positionX(), bounds().positionY(), bounds().width(), LINE_GAP);
        }

        @Override
        public boolean blocksHitWhenEmpty() {
            return true;
        }

        @Override
        public boolean onClick(float pointerX, float pointerY, int button) {
            return false;
        }
    }
}
