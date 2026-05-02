package cc.fascinated.fascinatedutils.gui.modsettings.components;

import cc.fascinated.fascinatedutils.gui.declare.Ui;
import cc.fascinated.fascinatedutils.gui.declare.UiComponent;
import cc.fascinated.fascinatedutils.gui.declare.UiView;
import cc.fascinated.fascinatedutils.gui.widgets.FAbsoluteStackWidget;
import cc.fascinated.fascinatedutils.gui.widgets.FWidget;

public final class ModSettingsSettingsPresentationComponent extends UiComponent<ModSettingsSettingsPresentationComponent.Props> {
    private static final float LAYOUT_STABILITY_EPSILON = 0.5f;

    public static UiView view(Props props) {
        return Ui.component(ModSettingsSettingsPresentationComponent.class, ModSettingsSettingsPresentationComponent::new, props);
    }

    @Override
    public UiView render() {
        Props snapshot = props();
        return Ui.custom(previousWidget -> {
            if (previousWidget instanceof PresentationSurface retained
                    && retained.matches(snapshot.presentationStamp(), snapshot.viewportWidth(), snapshot.viewportHeight())) {
                return retained;
            }
            FWidget nextSurface = snapshot.host().composeSettingsPresentationSurface(snapshot.viewportWidth(), snapshot.viewportHeight());
            if (!(nextSurface instanceof PresentationSurface surface)) {
                throw new IllegalStateException("composeSettingsPresentationSurface must return PresentationSurface");
            }
            surface.capture(snapshot.presentationStamp(), snapshot.viewportWidth(), snapshot.viewportHeight());
            return surface;
        });
    }

    public static PresentationSurface presentationSurfaceShell() {
        return new PresentationSurface();
    }

    public static final class PresentationSurface extends FAbsoluteStackWidget {
        private int recordedStamp = Integer.MIN_VALUE;
        private float recordedWidth = Float.NaN;
        private float recordedHeight = Float.NaN;

        public boolean matches(int presentationStamp, float viewportWidth, float viewportHeight) {
            return recordedStamp == presentationStamp
                    && Math.abs(recordedWidth - viewportWidth) < LAYOUT_STABILITY_EPSILON
                    && Math.abs(recordedHeight - viewportHeight) < LAYOUT_STABILITY_EPSILON;
        }

        public void capture(int presentationStamp, float viewportWidth, float viewportHeight) {
            recordedStamp = presentationStamp;
            recordedWidth = viewportWidth;
            recordedHeight = viewportHeight;
        }
    }

    public interface HostSurface {
        FWidget composeSettingsPresentationSurface(float viewportWidth, float viewportHeight);
    }

    public record Props(HostSurface host, float viewportWidth, float viewportHeight, int presentationStamp) {
    }
}
