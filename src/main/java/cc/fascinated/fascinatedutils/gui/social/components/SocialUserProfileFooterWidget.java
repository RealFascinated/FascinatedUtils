package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.api.Alumite;
import cc.fascinated.fascinatedutils.api.user.Activity;
import cc.fascinated.fascinatedutils.api.user.User;
import cc.fascinated.fascinatedutils.api.user.UserStatus;
import cc.fascinated.fascinatedutils.gui.core.UiFrameContext;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.renderer.UIRenderer;
import cc.fascinated.fascinatedutils.gui.themes.FascinatedGuiTheme;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public class SocialUserProfileFooterWidget extends FWidget {

    private static final float AVATAR_SIZE = 24f;

    private final float panelPadding;
    private final SocialPlayerAvatarWidget avatar;

    public SocialUserProfileFooterWidget(float panelPadding) {
        this.panelPadding = panelPadding;
        avatar = new SocialPlayerAvatarWidget(
                AVATAR_SIZE,
                () -> {
                    User selfUser = Alumite.INSTANCE.users().selfUser().user();
                    return selfUser != null ? selfUser.minecraftUuid() : null;
                },
                () -> {
                    User selfUser = Alumite.INSTANCE.users().selfUser().user();
                    return selfUser != null ? selfUser.minecraftName() : null;
                },
                () -> {
                    UserStatus status = Alumite.INSTANCE.users().selfUser().preferredUserStatus();
                    return status != null ? status.color() : UserStatus.OFFLINE.color();
                });
        addChild(avatar);
    }

    @Override
    public void layout(UIRenderer measure, float lx, float ly, float lw, float lh) {
        setBounds(lx, ly, lw, lh);
        avatar.layout(measure, lx, ly + (lh - AVATAR_SIZE) / 2f, AVATAR_SIZE, AVATAR_SIZE);
    }

    @Override
    protected void renderSelf(GuiRenderer graphics, UiFrameContext frame, float deltaSeconds) {
        graphics.drawRect(x() - panelPadding, y() - 1f, w() + panelPadding * 2f, 1f, 0x22FFFFFF);
        User selfUser = Alumite.INSTANCE.users().selfUser().user();
        String name = selfUser != null && selfUser.minecraftName() != null ? selfUser.minecraftName() : "";
        Activity selfActivity = Alumite.INSTANCE.users().selfUser().activity();
        float capHeight = graphics.getFontCapHeight();
        float textX = x() + AVATAR_SIZE + 8f;
        if (selfActivity != null) {
            float blockH = capHeight * 2 + 3f;
            float nameY = y() + (h() - blockH) / 2f;
            graphics.drawText(name, textX, nameY, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
            graphics.drawText(selfActivity.label(), textX, nameY + capHeight + 3f, FascinatedGuiTheme.INSTANCE.textMuted(), false, false);
        } else {
            graphics.drawText(name, textX, y() + (h() - capHeight) / 2f, FascinatedGuiTheme.INSTANCE.textPrimary(), true, false);
        }
    }
}
