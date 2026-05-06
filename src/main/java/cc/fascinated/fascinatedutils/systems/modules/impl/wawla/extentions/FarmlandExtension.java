package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.client.resources.language.I18n;
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
        return List.of(I18n.get("alumite.wawla.farmland.moisture", moisture, FarmlandBlock.MAX_MOISTURE));
    }
}
