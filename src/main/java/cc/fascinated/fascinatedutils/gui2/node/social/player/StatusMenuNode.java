package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.Constants;
import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui2.node.ContextMenuNode;
import cc.fascinated.fascinatedutils.oldgui.toast.Toast;
import net.minecraft.network.chat.Component;

import java.util.List;

public class StatusMenuNode extends ContextMenuNode {

    private static final List<UserStatus> SELECTABLE_STATUSES = List.of(
            UserStatus.ONLINE,
            UserStatus.AWAY,
            UserStatus.DO_NOT_DISTURB,
            UserStatus.INVISIBLE
    );

    public StatusMenuNode(Runnable onClose) {
        setOnClose(onClose);
        setItems(SELECTABLE_STATUSES.stream()
                .map(status -> new Item(
                        status.label(),
                        theme -> status.color(),
                        status.icon(),
                        6,
                        () -> {
                            onClose.run();
                            submitUpdate(status);
                        }))
                .toList());
    }

    private static void submitUpdate(UserStatus status) {
        Constants.EXECUTORS.execute(() -> {
            try {
                Alumite.INSTANCE.users().selfUser().updatePreferredUserStatus(status);
            } catch (Exception exception) {
                Toast.show().message(Component.translatable("alumite.social.error.generic").getString()).error();
            }
        });
    }
}
