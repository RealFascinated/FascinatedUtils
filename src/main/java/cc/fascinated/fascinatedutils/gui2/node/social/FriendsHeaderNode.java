package cc.fascinated.fascinatedutils.gui2.node.social;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.gui2.node.TextNode;
import cc.fascinated.fascinatedutils.gui2.theme.UiTheme;
import net.minecraft.network.chat.Component;

public class FriendsHeaderNode extends SocialPanelHeaderNode<FriendsHeaderNode> {

    public FriendsHeaderNode() {
        int count = Alumite.INSTANCE != null ? Alumite.INSTANCE.users().getFriends().size() : 0;
        String heading = Component.translatable("alumite.social.friends_heading").getString() + " (" + count + ")";
        TextNode title = new TextNode(() -> heading)
                .setColorResolver(UiTheme::textPrimary)
                .setTextAlign(0f, 0.5f);
        title.left(12).right(32).top(0).bottom(0);
        addChild(title);
    }
}
