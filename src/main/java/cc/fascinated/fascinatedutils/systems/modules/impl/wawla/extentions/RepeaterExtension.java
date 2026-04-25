package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RepeaterExtension extends WawlaBlockExtension<RepeaterBlock> {
    public RepeaterExtension(RepeaterBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int ticks = blockState.getValue(RepeaterBlock.DELAY) * 2;
        return List.of("Delay: " + ticks + " ticks");
    }
}
