package cc.fascinated.fascinatedutils.gui.social.components;

import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiView;

/**
 * Screen-level mount node for the social UI. Delegates to {@link SocialMainWorkspaceComponent}, which
 * retains selection, loading, and overlay state.
 */
public class SocialRootComponent extends UiComponent<SocialMainWorkspaceComponent.Props> {

    /**
     * Mounts the social screen root, which delegates rendering to {@link SocialMainWorkspaceComponent}.
     *
     * @param props viewport size, persisted inputs, close action, and presence hit-test sinks from the host screen
     * @return declarative view for the reconciler
     */
    public static UiView view(SocialMainWorkspaceComponent.Props props) {
        return Ui.component(SocialRootComponent.class, SocialRootComponent::new, props);
    }

    @Override
    public UiView render() {
        return SocialMainWorkspaceComponent.view(props());
    }
}
