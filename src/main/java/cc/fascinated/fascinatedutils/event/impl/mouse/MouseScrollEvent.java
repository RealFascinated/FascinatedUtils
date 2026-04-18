package cc.fascinated.fascinatedutils.event.impl.mouse;

import cc.fascinated.fascinatedutils.event.CancellableFascinatedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class MouseScrollEvent extends CancellableFascinatedEvent {
    private final long windowHandle;
    private final double horizontalScroll;
    private final double verticalScroll;
}
