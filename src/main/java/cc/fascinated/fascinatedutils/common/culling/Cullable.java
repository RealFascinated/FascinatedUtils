package cc.fascinated.fascinatedutils.common.culling;

/**
 * Interface injected into Entity and BlockEntity to track culling state.
 */
public interface Cullable {

    /**
     * Sets the culled state of this entity/block entity.
     *
     * @param culled whether this should be culled from rendering
     */
    void fascinatedutils$setCulled(boolean culled);

    /**
     * Gets whether this entity/block entity is currently culled.
     *
     * @return true if culled and should skip rendering
     */
    boolean fascinatedutils$isCulled();

    /**
     * Sets whether this entity is outside the camera view.
     *
     * @param outOfCamera true if outside camera frustum
     */
    void fascinatedutils$setOutOfCamera(boolean outOfCamera);

    /**
     * Gets whether this entity is outside the camera view.
     *
     * @return true if outside camera
     */
    boolean fascinatedutils$isOutOfCamera();
}
