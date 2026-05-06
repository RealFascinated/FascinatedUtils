package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.client.resources.language.I18n;
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
            case 0 -> I18n.get("alumite.wawla.sweet_berry.none");
            case 1 -> I18n.get("alumite.wawla.sweet_berry.growing");
            case 2 -> I18n.get("alumite.wawla.sweet_berry.some");
            default -> I18n.get("alumite.wawla.sweet_berry.full");
        });
    }
}
