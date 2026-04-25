package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ComparatorMode;

import java.util.List;

public class ComparatorExtension extends WawlaBlockExtension<ComparatorBlock> {
    public ComparatorExtension(ComparatorBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        ComparatorMode mode = blockState.getValue(ComparatorBlock.MODE);
        boolean powered = blockState.getValue(ComparatorBlock.POWERED);
        String modeLabel = mode == ComparatorMode.SUBTRACT ? "Subtract" : "Compare";
        return List.of("Mode: " + modeLabel, powered ? "Powered: On" : "Powered: Off");
    }
}
