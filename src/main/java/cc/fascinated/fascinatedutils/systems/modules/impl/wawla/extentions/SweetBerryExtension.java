package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SweetBerryExtension extends WawlaBlockExtension<SweetBerryBushBlock> {
    public SweetBerryExtension(SweetBerryBushBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        return List.of(switch (blockState.getValue(SweetBerryBushBlock.AGE)) {
            case 0 -> "Berries: None";
            case 1 -> "Berries: Growing";
            case 2 -> "Berries: Some";
            default -> "Berries: Full";
        });
    }
}
