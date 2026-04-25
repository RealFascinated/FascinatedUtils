package cc.fascinated.fascinatedutils.systems.modules.impl.wawla.extentions;

import cc.fascinated.fascinatedutils.systems.modules.impl.wawla.WawlaBlockExtension;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class NetherWartExtension extends WawlaBlockExtension<NetherWartBlock> {
    public NetherWartExtension(NetherWartBlock block) {
        super(block);
    }

    @Override
    public List<String> getExtension(BlockState blockState) {
        int age = blockState.getValue(NetherWartBlock.AGE);
        if (age >= NetherWartBlock.MAX_AGE) {
            return List.of("Growth: Fully grown");
        }
        int percent = Math.round((age / (float) NetherWartBlock.MAX_AGE) * 100f);
        return List.of("Growth: " + percent + "%");
    }
}
