package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class ComposterExtension extends WawlaBlockExtension<ComposterBlock> {
    public ComposterExtension(ComposterBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int level = blockState.getValue(ComposterBlock.LEVEL);
        if (level >= ComposterBlock.READY) {
            return List.of("Compost: Ready");
        }
        int percent = Math.round((level / (float) ComposterBlock.MAX_LEVEL) * 100f);
        return List.of("Compost: " + percent + "%");
    }
}
