package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.ChannelMessage;
import cc.fascinated.fascinatedutils.api.channel.json.ChannelMessagePageDTO;
import cc.fascinated.fascinatedutils.gui.core.Align;
import cc.fascinated.fascinatedutils.gui.core.FState;
import cc.fascinated.fascinatedutils.gui.core.GuiFocusState;
import cc.fascinated.fascinatedutils.gui.core.PointerHitKind;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.toast.Toast;
import cc.fascinated.fascinatedutils.gui.widgets.FColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FContextMenuWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FOutlinedTextInputWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FScrollColumnWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
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

    private Integer loadingMessagesChannelId;
    private Integer loadingOlderMessagesChannelId;
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
    List<FWidget> collectOverlayWidgets(Integer selectedChannelId) {
        List<FWidget> result = new ArrayList<>();
        if (contextMenuMessage != null) {
            result.add(buildContextMenuOverlay());
        }
        if (pendingDeleteMessage != null) {
            ChannelMessage toDelete = pendingDeleteMessage;
            result.add(SocialDestructiveFullscreenConfirmOverlay.create(new SocialDestructiveFullscreenConfirmOverlay.Props(
                    Component.translatable("fascinatedutils.social.delete_message.title").getString(),
                    Component.translatable("fascinatedutils.social.delete_message.message").getString(),
                    Component.translatable("fascinatedutils.social.delete_message.confirm").getString(),
                    Component.translatable("fascinatedutils.social.delete_message.deny").getString(),
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
    FWidget buildMessagesBody(Integer selectedChannelId, float panelWidth, FState<Float> messageScrollY) {
        if (selectedChannelId == null) {
            return buildEmpty(Component.translatable("fascinatedutils.social.loading").getString());
        }
        Channel channel = Alumite.INSTANCE.channels().get(selectedChannelId);
        List<ChannelMessage> messages = channel == null ? null : channel.messagesOrNull();
        if (messages == null) {
            triggerLoadMessages(selectedChannelId);
            return buildEmpty(Component.translatable("fascinatedutils.social.loading").getString());
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

    private FWidget buildMessageRow(ChannelMessage message, float width, Integer selectedChannelId) {
        boolean own = Objects.equals(Alumite.INSTANCE.activeUserId(), message.authorId());
        boolean isEditing = editingMessage != null && editingMessage.id() == message.id();
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
                        () -> Component.translatable("fascinatedutils.social.edit_message.action").getString(),
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
                        () -> Component.translatable("fascinatedutils.social.delete_message.action").getString(),
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

    private void submitEdit(Integer selectedChannelId) {
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
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                channel.editMessage(target.id(), newContent);
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }
            Minecraft.getInstance().execute(() -> {
                editMessagePending = false;
                editingMessage = null;
            });
        });
    }

    private void submitDelete(ChannelMessage message, Integer selectedChannelId) {
        Channel channel = selectedChannelId != null ? Alumite.INSTANCE.channels().get(selectedChannelId) : null;
        if (channel == null) {
            return;
        }
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                channel.deleteMessage(message.id());
            } catch (AlumiteApiException exception) {
                Toast.show().message(SocialErrors.message(exception)).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }
        });
    }

    private void triggerLoadMessages(int channelId) {
        if (loadingMessagesChannelId != null && loadingMessagesChannelId == channelId) {
            return;
        }
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        loadingMessagesChannelId = channelId;
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                channel.fetchMessages(50);
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("fascinatedutils.social.error.generic").getString()).error();
            }
            Minecraft.getInstance().execute(() -> loadingMessagesChannelId = null);
        });
    }

    private void triggerLoadOlderMessages(int channelId) {
        if (loadingOlderMessagesChannelId != null && loadingOlderMessagesChannelId == channelId) {
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
        int oldestMessageId = existingMessages.get(0).id();
        loadingOlderMessagesChannelId = channelId;
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
            try {
                ChannelMessagePageDTO page = channel.fetchMessagesPage(50, oldestMessageId, null);
                channel.mergeOlderMessagesPage(page);
            } catch (Exception ignored) {
            }
            Minecraft.getInstance().execute(() -> loadingOlderMessagesChannelId = null);
        });
    }

    private void markChannelRead(int channelId) {
        Channel channel = Alumite.INSTANCE.channels().get(channelId);
        if (channel == null) {
            return;
        }
        List<ChannelMessage> messages = channel.messagesOrNull();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        int lastMessageId = messages.get(messages.size() - 1).id();
        Integer lastReadMessageId = channel.lastReadMessageId();
        if (lastReadMessageId != null && lastReadMessageId >= lastMessageId) {
            return;
        }
        FascinatedUtils.SCHEDULED_POOL.execute(() -> {
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
