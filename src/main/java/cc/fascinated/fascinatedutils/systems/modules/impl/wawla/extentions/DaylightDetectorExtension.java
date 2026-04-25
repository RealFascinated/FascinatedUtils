package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.DaylightDetectorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class DaylightDetectorExtension extends WawlaBlockExtension<DaylightDetectorBlock> {
    public DaylightDetectorExtension(DaylightDetectorBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int power = blockState.getValue(DaylightDetectorBlock.POWER);
        boolean inverted = blockState.getValue(DaylightDetectorBlock.INVERTED);
        return List.of((inverted ? "Night Signal: " : "Signal: ") + power + "/15");
    }
}
