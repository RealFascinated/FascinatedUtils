package cc.fascinated.fascinatedutils.event;

import lombok.Getter;
import lombok.Setter;
import meteordevelopment.orbit.ICancellable;

@Getter
@Setter
public abstract class CancellableFascinatedEvent implements ICancellable {
    private boolean cancelled;
}
