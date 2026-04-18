package cc.fascinated.fascinatedutils.event.impl.render;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.Camera;

@Getter
@Accessors(fluent = true)
public class ClientFovEvent {
    private final Camera camera;
    private final float tickProgress;
    private final boolean changingFov;
    private final float vanillaFovDegrees;
    private float fovDegrees;

    /**
     * Create an FOV event carrying vanilla output before mod listeners run.
     *
     * @param camera            camera passed to {@code GameRenderer#getFov}
     * @param tickProgress      partial tick passed to {@code GameRenderer#getFov}
     * @param changingFov       changing-FOV flag passed to {@code GameRenderer#getFov}
     * @param vanillaFovDegrees FOV in degrees from vanilla before mod adjustment
     */
    public ClientFovEvent(Camera camera, float tickProgress, boolean changingFov, float vanillaFovDegrees) {
        this.camera = camera;
        this.tickProgress = tickProgress;
        this.changingFov = changingFov;
        this.vanillaFovDegrees = vanillaFovDegrees;
        this.fovDegrees = vanillaFovDegrees;
    }

    /**
     * Set the FOV in degrees that {@code GameRenderer#getFov} should return after listeners run.
     *
     * @param fovDegrees adjusted horizontal FOV in degrees
     */
    public void setFovDegrees(float fovDegrees) {
        this.fovDegrees = fovDegrees;
    }
}
