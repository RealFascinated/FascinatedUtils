package cc.fascinated.fascinatedutils.turboentities;

import com.logisticscraft.occlusionculling.DataProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;

class OcclusionProvider implements DataProvider {

    private final Minecraft client = Minecraft.getInstance();
    private ClientLevel world = null;

    @Override
    public boolean prepareChunk(int chunkX, int chunkZ) {
        world = client.level;
        return world != null;
    }

    @Override
    public boolean isOpaqueFullCube(int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).isSolidRender();
    }

    @Override
    public void cleanup() {
        world = null;
    }
}
