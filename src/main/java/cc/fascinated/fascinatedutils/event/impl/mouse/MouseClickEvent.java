package cc.fascinated.fascinatedutils.event.impl.mouse;

import cc.fascinated.fascinatedutils.event.CancellableFascinatedEvent;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import net.minecraft.client.input.MouseButtonInfo;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class MouseClickEvent extends CancellableFascinatedEvent {
    private final long windowHandle;
    private final MouseButtonInfo mouseInput;
    private final int action;
}
