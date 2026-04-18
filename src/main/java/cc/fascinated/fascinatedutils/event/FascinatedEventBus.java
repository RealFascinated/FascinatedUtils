package cc.fascinated.fascinatedutils.event;

import cc.fascinated.fascinatedutils.client.Client;
import lombok.Getter;
import meteordevelopment.orbit.EventBus;
import meteordevelopment.orbit.ICancellable;

import java.lang.invoke.MethodHandles;

@Getter
public class FascinatedEventBus {
    public static final FascinatedEventBus INSTANCE = new FascinatedEventBus();

    private final EventBus bus = new EventBus();
    private boolean setupDone;

    private FascinatedEventBus() {
    }

    /**
     * Register Orbit lambda access for {@code cc.fascinated.fascinatedutils}. Call this before subscribing any
     * listeners from {@code cc.fascinated.fascinatedutils} (see {@link Client#onInitializeClient()}).
     */
    public void ensureSetup() {
        if (setupDone) {
            return;
        }
        setupDone = true;
        bus.registerLambdaFactory("cc.fascinated.fascinatedutils", (lookupInMethod, klass) -> {
            try {
                return (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup());
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /**
     * Register an instance for {@link meteordevelopment.orbit.EventHandler} listener methods.
     *
     * @param subscriber object whose annotated methods should receive events
     */
    public void subscribe(Object subscriber) {
        bus.subscribe(subscriber);
    }

    /**
     * Register static {@link meteordevelopment.orbit.EventHandler} methods on a class.
     *
     * @param klass class whose static annotated methods should receive events
     */
    public void subscribe(Class<?> klass) {
        bus.subscribe(klass);
    }

    /**
     * Post a non-cancellable event to all subscribed listeners.
     *
     * @param event event instance to deliver
     * @param <T>   event type
     * @return the same event instance after listeners run
     */
    public <T> T post(T event) {
        return bus.post(event);
    }

    /**
     * Post a cancellable event using Orbit's cancellable dispatch (stops after cancellation, resets cancelled flag).
     *
     * @param event event instance to deliver
     * @param <T>   cancellable event type
     * @return the same event instance after listeners run
     */
    public <T extends ICancellable> T postCancellable(T event) {
        return bus.post(event);
    }
}
