package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.client.resources.language.I18n;
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
        String modeLabel = mode == ComparatorMode.SUBTRACT
                ? I18n.get("alumite.wawla.comparator.mode.subtract")
                : I18n.get("alumite.wawla.comparator.mode.compare");
        String poweredLabel = powered
                ? I18n.get("alumite.wawla.comparator.powered.on")
                : I18n.get("alumite.wawla.comparator.powered.off");
        return List.of(modeLabel, poweredLabel);
    }
}
