package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.gui.widgets.FContextMenuWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

class UserContextMenuWidget {

    static FWidget build(float posX, float posY, User user, Runnable onClose, Runnable onRemoveFriend) {
        List<FContextMenuWidget.Item> items = new ArrayList<>();

        items.add(new FContextMenuWidget.Item(
                () -> Component.translatable("alumite.social.user_context_menu.copy_name").getString(),
                () -> {
                    if (user.minecraftName() != null) {
                        ClientUtils.copyToClipboard(user.minecraftName());
                    }
                    onClose.run();
                }));

        items.add(new FContextMenuWidget.Item(
                () -> Component.translatable("alumite.social.user_context_menu.copy_uuid").getString(),
                () -> {
                    if (user.minecraftUuid() != null) {
                        ClientUtils.copyToClipboard(user.minecraftUuid());
                    }
                    onClose.run();
                }));

        boolean isFriend = Alumite.INSTANCE.users().friends().stream()
                .anyMatch(friend -> friend.user().id().equals(user.id()));
        if (isFriend && onRemoveFriend != null) {
            items.add(new FContextMenuWidget.Item(
                    () -> Component.translatable("alumite.social.user_context_menu.remove_friend").getString(),
                    0xFFFF5555,
                    () -> {
                        onClose.run();
                        onRemoveFriend.run();
                    }));
        }

        return new FContextMenuWidget(posX, posY, onClose, items);
    }
}
