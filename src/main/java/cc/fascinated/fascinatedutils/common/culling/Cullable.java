package cc.fascinated.fascinatedutils.common.culling;

/**
 * Interface injected into Entity, BlockEntity and Particle to track culling state.
 */
public interface Cullable {

    /**
     * Sets the culled state of this entity, block entity or particle.
     *
     * @param culled whether this should be culled from rendering
     */
    void alumite$setCulled(boolean culled);

    /**
     * Gets whether this entity, block entity or particle is currently culled.
     *
     * @return true if culled and should skip rendering
     */
    boolean alumite$isCulled();
}
