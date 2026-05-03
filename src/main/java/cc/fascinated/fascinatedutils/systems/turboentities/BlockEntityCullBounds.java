package cc.fascinated.fascinatedutils.systems.turboentities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * Tight culling boxes for block entities; large inflates interact badly with occlusion fast-paths.
 */
public final class BlockEntityCullBounds {

    private BlockEntityCullBounds() {
    }

    /**
     * Returns an axis-aligned box used for frustum and occlusion tests (not for rendering).
     *
     * @param blockEntity block entity instance
     * @param pos         block position of the block entity
     * @return bounding box in world space
     */
    public static AABB forCull(BlockEntity blockEntity, BlockPos pos) {
        if (blockEntity instanceof BannerBlockEntity) {
            return new AABB(pos).inflate(0, 1, 0);
        }
        return new AABB(pos);
    }
}
