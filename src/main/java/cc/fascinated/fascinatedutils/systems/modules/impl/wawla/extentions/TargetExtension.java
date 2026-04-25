package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.TargetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.List;

public class TargetExtension extends WawlaBlockExtension<TargetBlock> {
    public TargetExtension(TargetBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int power = blockState.getValue(BlockStateProperties.POWER);
        return power == 0 ? List.of() : List.of("Signal: " + power + "/15");
    }
}
