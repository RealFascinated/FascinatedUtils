package cc.fascinated.fascinatedutils.gui2.node.social.player;

import cc.fascinated.fascinatedutils.api.user.User;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface PlayerContextMenuHandler {
    void open(@Nullable User user, float pointerX, float pointerY);
}
