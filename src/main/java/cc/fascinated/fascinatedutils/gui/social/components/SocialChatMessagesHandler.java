package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.AlumiteMod;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessagePageDTO;
import cc.fascinated.fascinatedutils.gui.core.*;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.gui.widgets.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Retained helper that owns all per-channel message interaction state: context
 * menus, edit-message flow, delete confirmation, and scroll-position tracking.
 *
 * <p>Instantiated once as a field of {@link SocialMainWorkspaceComponent} so
 * its state survives across re-renders. Call
 * {@link #buildMessagesBody(Integer, float, FState)} to obtain the scroll widget
 * for the chat body, and {@link #collectOverlayWidgets(Integer)} to obtain any
 * active overlay widgets to stack above the chat body.</p>
 */
class SocialChatMessagesHandler {
    private static final float MESSAGE_SCROLL_EDGE_EPSILON = 0.01f;
    static final float MESSAGE_SCROLL_ANCHOR_BOTTOM = Float.MAX_VALUE;
    private static final float PAD = 10f;

    private String loadingMessagesChannelId;
    private String loadingOlderMessagesChannelId;
    private ChannelMessage contextMenuMessage;
    private float contextMenuX;
    private float contextMenuY;
    private ChannelMessage pendingDeleteMessage;
    private ChannelMessage editingMessage;
    private final FOutlinedTextInputWidget editMessageInput = new FOutlinedTextInputWidget(4000, 20f, () -> "");
    private boolean editMessagePending;

    boolean hasActiveOverlay() {
        return pendingDeleteMessage != null;
    }

    void suppressContextualOverlays() {
        contextMenuMessage = null;
    }

    /**
     * Returns any active overlay widgets (context menu, delete confirmation).
     * Called from the parent layout pass to stack overlays on top of the chat body.
     */
    List<FWidget> collectOverlayWidgets(String selectedChannelId) {
        List<FWidget> result = new ArrayList<>();
        if (contextMenuMessage != null) {
            result.add(buildContextMenuOverlay());
        }
        if (pendingDeleteMessage != null) {
            ChannelMessage toDelete = pendingDeleteMessage;
            result.add(SocialDestructiveFullscreenConfirmOverlay.create(new SocialDestructiveFullscreenConfirmOverlay.Props(
                    Component.translatable("alumite.social.delete_message.title").getString(),
                    Component.translatable("alumite.social.delete_message.message").getString(),
                    Component.translatable("alumite.social.delete_message.confirm").getString(),
                    Component.translatable("alumite.social.delete_message.deny").getString(),
                    () -> pendingDeleteMessage = null,
                    () -> {
                        pendingDeleteMessage = null;
                        submitDelete(toDelete, selectedChannelId);
                    })));
        }
        return result;
    }

    /**
     * Builds the scrollable messages body for the given channel.
     *
     * @param selectedChannelId channel to display; a loading placeholder is shown when null
     * @param panelWidth        available width for the messages area
     * @param messageScrollY    scroll position state; {@link Float#MAX_VALUE} pins to bottom
     */
    FWidget buildMessagesBody(String selectedChannelId, float panelWidth, FState<Float> messageScrollY) {
        if (selectedChannelId == null) {
            return buildEmpty(Component.translatable("alumite.social.loading").getString());
        }
        Channel channel = Alumite.INSTANCE.channels().get(selectedChannelId);
        List<ChannelMessage> messages = channel == null ? null : channel.messagesOrNull();
        if (messages == null) {
            triggerLoadMessages(selectedChannelId);
            return buildEmpty(Component.translatable("alumite.social.loading").getString());
        }

        float messageWidth = panelWidth - 2f * PAD;
        FColumnWidget body = new FColumnWidget(5f, Align.START);
        for (ChannelMessage message : messages) {
                body.addChild(buildMessageRow(message, messageWidth, selectedChannelId));
            }

        FScrollColumnWidget scroll = FTheme.components().createScrollColumn(body, 3f);
        Float savedScroll = messageScrollY.get();
        boolean anchorToBottom = savedScroll == null || savedScroll == MESSAGE_SCROLL_ANCHOR_BOTTOM;
        scroll.setPinBodyToBottomWhenContentFits(true);
        scroll.setScrollOffsetY(anchorToBottom ? MESSAGE_SCROLL_ANCHOR_BOTTOM : (savedScroll == null ? 0f : savedScroll));
        scroll.setScrollOffsetChangeListener(offset -> {
            boolean atBottom = isAtBottom(scroll, offset);
            if (atBottom) {
                messageScrollY.setQuiet(MESSAGE_SCROLL_ANCHOR_BOTTOM);
                markChannelRead(selectedChannelId);
            } else {
                messageScrollY.setQuiet(offset);
            }
            if (offset <= MESSAGE_SCROLL_EDGE_EPSILON) {
                triggerLoadOlderMessages(selectedChannelId);
            }
        });
        return scroll;
    }

    private boolean isAtBottom(FScrollColumnWidget scroll, float offset) {
        float maxOffset = Math.max(0f, scroll.contentHeight() - scroll.h());
        return maxOffset <= MESSAGE_SCROLL_EDGE_EPSILON || offset >= maxOffset - MESSAGE_SCROLL_EDGE_EPSILON;
    }

    private FWidget buildMessageRow(ChannelMessage message, float width, String selectedChannelId) {
        boolean own = Objects.equals(Alumite.INSTANCE.users().selfUser().user().id(), message.authorId());
        boolean isEditing = editingMessage != null && editingMessage.id().equals(message.id());
        BiConsumer<Float, Float> onContextMenu = own && !isEditing ? (mx, my) -> {
            contextMenuMessage = message;
            contextMenuX = mx;
            contextMenuY = my;
        } : null;
        FOutlinedTextInputWidget editInput = isEditing ? editMessageInput : null;
        Runnable onSaveEdit = isEditing ? () -> submitEdit(selectedChannelId) : null;
        Runnable onCancelEdit = isEditing ? () -> editingMessage = null : null;
        return SocialMessageBubbleWidget.build(new SocialMessageBubbleWidget.Props(message, own, onContextMenu, editInput, onSaveEdit, onCancelEdit), width);
    }

    private FWidget buildContextMenuOverlay() {
        FContextMenuWidget menu = new FContextMenuWidget(contextMenuX, contextMenuY, () -> contextMenuMessage = null, List.of(
                new FContextMenuWidget.Item(
                        () -> Component.translatable("alumite.social.edit_message.action").getString(),
                        () -> {
                            ChannelMessage target = contextMenuMessage;
                            contextMenuMessage = null;
                            if (target != null) {
                                editingMessage = target;
                                editMessageInput.setValue(target.content());
                                GuiFocusState.setFocusedId(editMessageInput.focusId());
                            }
                        }),
                new FContextMenuWidget.Item(
                        () -> Component.translatable("alumite.social.delete_message.action").getString(),
                        0xFFFF5555,
                        () -> {
                            ChannelMessage target = contextMenuMessage;
                            contextMenuMessage = null;
                            if (target != null) {
                                pendingDeleteMessage = target;
                            }
                        })
        ));
        return new FWidget() {
            {
                addChild(menu);
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
                menu.layout(measure, lx, ly, lw, lh);
            }

            @Override
            public PointerHitKind pointerHitKind() {
                return PointerHitKind.BLOCK;
            }

            @Override
            public void render(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                if (!visible()) {
                    return;
                }
                graphics.absolutePost(() -> menu.render(graphics, frame, deltaSeconds));
            }

            @Override
            public boolean mouseDown(float pointerX, float pointerY, int button) {
                return menu.mouseDown(pointerX, pointerY, button);
            }

            @Override
            public boolean click(float pointerX, float pointerY, int button) {
                return menu.click(pointerX, pointerY, button);
            }
        };
    }

    private void submitEdit(String selectedChannelId) {
        ChannelMessage target = editingMessage;
        if (target == null || editMessagePending) {
            return;
        }
        String newContent = editMessageInput.value().trim();
        if (newContent.isEmpty() || newContent.equals(target.content())) {
            editingMessage = null;
            return;
        }
        editMessagePending = true;
        Channel channel = selectedChannelId != null ? Alumite.INSTANCE.channels().get(selectedChannelId) : null;
        if (channel == null) {
            editMessagePending = false;
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.editMessage(target.id(), newContent);
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
            Minecraft.getInstance().execute(() -> {
                editMessagePending = false;
                editingMessage = null;
            });
        });
    }

    private void submitDelete(ChannelMessage message, String selectedChannelId) {
        Channel channel = selectedChannelId != null ? Alumite.INSTANCE.channels().get(selectedChannelId) : null;
        if (channel == null) {
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.deleteMessage(message.id());
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }

    private void triggerLoadMessages(String channelId) {
        if (loadingMessagesChannelId != null && loadingMessagesChannelId.equals(channelId)) {
            return;
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        loadingMessagesChannelId = channelId;
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.fetchMessages(50);
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
            Minecraft.getInstance().execute(() -> loadingMessagesChannelId = null);
        });
    }

    private void triggerLoadOlderMessages(String channelId) {
        if (loadingOlderMessagesChannelId != null && loadingOlderMessagesChannelId.equals(channelId)) {
            return;
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        if (!channel.hasMoreMessages()) {
            return;
        }
        List<ChannelMessage> existingMessages = channel.messagesOrNull();
        if (existingMessages == null || existingMessages.isEmpty()) {
            return;
        }
        String oldestMessageId = existingMessages.get(0).id();
        loadingOlderMessagesChannelId = channelId;
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                ChannelMessagePageDTO page = channel.fetchMessagesPage(50, oldestMessageId, null);
                channel.mergeOlderMessagesPage(page);
            } catch (Exception ignored) {
            }
            Minecraft.getInstance().execute(() -> loadingOlderMessagesChannelId = null);
        });
    }

    private void markChannelRead(String channelId) {
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        List<ChannelMessage> messages = channel.messagesOrNull();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        String lastMessageId = messages.get(messages.size() - 1).id();
        String lastReadMessageId = channel.lastReadMessageId();
        if (lastReadMessageId != null && lastReadMessageId.compareTo(lastMessageId) >= 0) {
            return;
        }
        AlumiteMod.SCHEDULED_POOL.execute(() -> {
            try {
                channel.markRead(lastMessageId);
            } catch (Exception ignored) {
            }
        });
    }

    private FWidget buildEmpty(String message) {
        return new FWidget() {
            @Override
            public float intrinsicHeightForColumn(UIRenderer measure, float widthBudget) {
                return measure.getFontCapHeight() + 8f;
            }

            @Override
            public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
                setBounds(lx, ly, lw, lh);
            }

            @Override
            protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
                graphics.drawCenteredText(message, x() + w() / 2f, y() + 4f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
            }
        };
    }
}
