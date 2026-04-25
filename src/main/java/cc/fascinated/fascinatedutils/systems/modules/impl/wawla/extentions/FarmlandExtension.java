package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FarmlandExtension extends WawlaBlockExtension<FarmlandBlock> {
    public FarmlandExtension(FarmlandBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int moisture = blockState.getValue(FarmlandBlock.MOISTURE);
        return List.of("Moisture: " + moisture + "/" + FarmlandBlock.MAX_MOISTURE);
    }
}
