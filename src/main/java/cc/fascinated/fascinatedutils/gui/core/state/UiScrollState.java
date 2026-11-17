package cc.fascinated.fascinatedutils.gui.core.state;

import lombok.Getter;
import lombok.Setter;

/**
 * Scroll position remembered for one persisted scroll container so it survives recomposition.
 */
@Getter
@Setter
public class UiScrollState {
    private boolean initialized;
    private int offset;
    private float visualOffset;
    private int lastLayoutMicros;
    private boolean hasLastLayout;
}
