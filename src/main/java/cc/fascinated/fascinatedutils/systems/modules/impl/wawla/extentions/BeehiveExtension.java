package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BeehiveExtension extends WawlaBlockExtension<BeehiveBlock> {
    public BeehiveExtension(BeehiveBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int level = blockState.getValue(BeehiveBlock.HONEY_LEVEL);
        return List.of("Honey: " + level + "/" + BeehiveBlock.MAX_HONEY_LEVELS);
    }
}
