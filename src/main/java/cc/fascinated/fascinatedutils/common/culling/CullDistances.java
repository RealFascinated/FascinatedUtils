package cc.fascinated.fascinatedutils.common.culling;

/**
 * Distances that decide how far the turbo systems cull. Squared values carry the {@code _SQ} suffix.
 */
public class CullDistances {
    /**
     * Boxes larger than this on any axis are never culled.
     */
    public static final double HITBOX_LIMIT = 64.0;
    public static final double ENTITY_FORCE_CULL_DISTANCE_SQ = 128.0 * 128.0;
    public static final double BLOCK_ENTITY_FORCE_CULL_DISTANCE_SQ = 64.0 * 64.0;
    public static final double PARTICLE_FORCE_CULL_DISTANCE_SQ = 64.0 * 64.0;
    public static final double ITEM_RENDER_DISTANCE_SQ = 32.0 * 32.0;
    public static final double DECORATION_RENDER_DISTANCE_SQ = 48.0 * 48.0;
}
