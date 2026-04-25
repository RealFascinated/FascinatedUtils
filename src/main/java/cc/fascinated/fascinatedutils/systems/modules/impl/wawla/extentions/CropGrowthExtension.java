package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class CropGrowthExtension extends WawlaBlockExtension<CropBlock> {
    public CropGrowthExtension(CropBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int age = getBlock().getAge(blockState);
        int maxAge = getBlock().getMaxAge();
        int percent = Math.round((age / (float) maxAge) * 100f);
        return List.of("Growth: " + percent + "%");
    }
}
