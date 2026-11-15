package cc.fascinated.fascinatedutils.common.culling;

import com.logisticscraft.occlusionculling.DataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

public class OcclusionProvider implements DataProvider {

    private final Minecraft client = Minecraft.getInstance();
    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private ClientLevel world = null;

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        world = client.level;
        return world != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        ClientLevel level = world;
        if (level == null) {
            return false;
        }
        return level.getBlockState(scratchPos.set(x, y, z)).isSolidRender();
    }

    @Override
    public void cleanup() {
        world = null;
    }
}
