package cc.fascinated.fascinatedutils.systems.modules.impl.wawla;

import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

@Getter
public abstract class WawlaBlockExtension<B extends Block> {
    private final B block;

    public WawlaBlockExtension(B block) {
        this.block = block;
    }

    /**
     * Returns extra lines to display for the given block state.
     * An empty list means nothing extra is shown.
     *
     * @param blockState the block state being looked at
     * @return list of extra display lines, empty if nothing to show
     */
    public abstract List<String> getExtension(BlockState blockState);
}
