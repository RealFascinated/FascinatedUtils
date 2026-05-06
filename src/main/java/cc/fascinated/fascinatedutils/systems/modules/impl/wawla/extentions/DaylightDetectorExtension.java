package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.client.resources.language.I18n;
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
        String key = inverted ? "alumite.wawla.daylight_detector.night_signal" : "alumite.wawla.daylight_detector.signal";
        return List.of(I18n.get(key, power));
    }
}
