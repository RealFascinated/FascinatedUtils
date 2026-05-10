package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.client.ModUiTextures;
import cc.fascinated.fascinatedutils.common.ClientUtils;
import cc.fascinated.fascinatedutils.gui2.node.ContextMenuNode;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PlayerContextMenuNode extends ContextMenuNode {

    public PlayerContextMenuNode(User user, Runnable onClose, Runnable onRemoveFriend) {
        setOnClose(onClose);
        setItems(buildItems(user, onClose, onRemoveFriend));
    }

    private static List<Item> buildItems(User user, Runnable onClose, Runnable onRemoveFriend) {
        List<Item> items = new ArrayList<>();

        items.add(new Item(
                Component.translatable("alumite.social.user_context_menu.copy_name").getString(),
                ModUiTextures.COPY.getId(),
                () -> {
                    if (user != null && user.minecraftName() != null) {
                        ClientUtils.copyToClipboard(user.minecraftName());
                    }
                    onClose.run();
                }));

        items.add(new Item(
                Component.translatable("alumite.social.user_context_menu.copy_uuid").getString(),
                ModUiTextures.COPY.getId(),
                () -> {
                    if (user != null && user.minecraftUuid() != null) {
                        ClientUtils.copyToClipboard(user.minecraftUuid());
                    }
                    onClose.run();
                }));

        if (user != null && Alumite.INSTANCE != null && Alumite.INSTANCE.users().isFriend(user.id()) && onRemoveFriend != null) {
            items.add(Item.separator());
            items.add(new Item(
                    Component.translatable("alumite.social.user_context_menu.remove_friend").getString(),
                    ModUiTextures.TRASH.getId(),
                    () -> {
                        onClose.run();
                        onRemoveFriend.run();
                    }));
        }

        return items;
    }
}