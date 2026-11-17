package cc.fascinated.fascinatedutils.gui.layout;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The per-axis constraints of one node. Nodes create it lazily, so nodes that never set a box property carry none.
 */
@Getter
@Accessors(fluent = true)
public class BoxLayoutSpec {
    private final AxisConstraints horizontal = new AxisConstraints();
    private final AxisConstraints vertical = new AxisConstraints();
}
