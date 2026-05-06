package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class RedstoneWireExtension extends WawlaBlockExtension<RedStoneWireBlock> {
    public RedstoneWireExtension(RedStoneWireBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int power = blockState.getValue(RedStoneWireBlock.POWER);
        return List.of(I18n.get("alumite.wawla.redstone_wire.power", power));
    }
}
