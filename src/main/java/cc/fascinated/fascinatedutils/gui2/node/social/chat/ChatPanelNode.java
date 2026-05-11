package cc.fascinated.fascinatedutils.gui2.node.social.chat;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.AlumiteApiException;
import cc.fascinated.fascinatedutils.api.channel.Channel;
import cc.fascinated.fascinatedutils.api.channel.DmChannel;
import cc.fascinated.fascinatedutils.api.channel.Message;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.gui2.core.PositionedNode;
import cc.fascinated.fascinatedutils.gui2.core.UiNode;
import cc.fascinated.fascinatedutils.gui2.core.UiState;
import cc.fascinated.fascinatedutils.gui2.core.UiStateStore;
import cc.fascinated.fascinatedutils.gui2.node.ConfirmPopupNode;
import cc.fascinated.fascinatedutils.gui2.node.social.player.PlayerContextMenuHandler;
import cc.fascinated.fascinatedutils.gui2.render.RenderFrame;
import cc.fascinated.fascinatedutils.oldgui.toast.Toast;
import net.minecraft.network.chat.Component;

public class ChatPanelNode extends PositionedNode<ChatPanelNode> {

    private static final int HEADER_HEIGHT = 32;
    private static final int PADDING = 6;

    private final ChatHeaderNode header;
    private final PositionedNode messagesWrapper;
    private final ChatComposerNode composer;

    public ChatPanelNode(Channel channel, UiStateStore stateStore, PlayerContextMenuHandler contextMenuHandler) {
        full();

        header = new ChatHeaderNode(() -> channel);
        header.setOnSecondaryClick((pointerX, pointerY) -> {
            DmChannel dm = channel.asDmChannel();
            User recipient = dm != null ? dm.recipient() : null;
            if (contextMenuHandler != null && recipient != null) {
                contextMenuHandler.open(recipient, pointerX, pointerY);
            }
        });
        addChild(header);

        messagesWrapper = new PositionedNode()
                .left(PADDING)
                .right(PADDING)
                .top(HEADER_HEIGHT + PADDING)
                .bottom(PADDING * 2);
        ChatMessagesNode messages = new ChatMessagesNode(channel, stateStore, contextMenuHandler);
        messagesWrapper.addChild(messages);
        addChild(messagesWrapper);

        composer = new ChatComposerNode(channel, stateStore, composerPlaceholder(channel));
        addChild(composer);

        UiState<Message> msgPendingDelete = stateStore.state("social.chat.delete.message", null);
        Message deleteMsg = msgPendingDelete.get();
        if (deleteMsg != null) {
            ConfirmPopupNode deleteConfirm = new ConfirmPopupNode()
                    .setTitle(Component.translatable("alumite.social.chat.delete_popup.title").getString())
                    .setConfirmLabel(Component.translatable("alumite.common.delete").getString())
                    .setConfirmLabelColorResolver(theme -> theme.danger())
                    .setOnCancel(() -> msgPendingDelete.set(null))
                    .setOnConfirm(() -> {
                        msgPendingDelete.set(null);
                        deleteMessage(channel, deleteMsg);
                    });
            addChild(deleteConfirm);
        }
    }

    @Override
    public void layout(RenderFrame frame, int parentX, int parentY, int parentWidth, int parentHeight) {
        int myWidth = boxLayout().horizontal().resolveSize(parentWidth);
        int myHeight = boxLayout().vertical().resolveSize(parentHeight);
        int myX = boxLayout().horizontal().resolvePosition(parentX, parentWidth, myWidth);
        int myY = boxLayout().vertical().resolvePosition(parentY, parentHeight, myHeight);
        bounds().set(myX, myY, myWidth, myHeight);

        int composerAvailableWidth = myWidth - 2 * PADDING;
        int composerHeight = composer.preferredHeight(frame, composerAvailableWidth);
        int composerY = myY + myHeight - composerHeight - PADDING;

        // Adjust messages wrapper bottom to sit above the composer
        messagesWrapper.bottom(myHeight - (composerY - myY) + PADDING);

        header.layout(frame, myX, myY, myWidth, myHeight);
        messagesWrapper.layout(frame, myX, myY, myWidth, myHeight);
        composer.layout(frame, myX + PADDING, composerY, composerAvailableWidth, myHeight);

        // Overlay children (context menu, confirm popup)
        for (UiNode child : childrenView()) {
            if (child == header || child == messagesWrapper || child == composer) {
                continue;
            }
            child.layout(frame, myX, myY, myWidth, myHeight);
        }
    }

    private static String composerPlaceholder(Channel channel) {
        if (channel instanceof DmChannel dm && dm.recipient() != null) {
            String name = dm.recipient().minecraftName();
            return "Message " + (name != null ? name : "them") + "...";
        }
        String name = channel.name();
        return "Message " + (name != null ? name : "channel") + "...";
    }

    private static void deleteMessage(Channel channel, Message message) {
        Constants.EXECUTORS.execute(() -> {
            try {
                channel.deleteMessage(message.id());
            } catch (AlumiteApiException exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }
}
