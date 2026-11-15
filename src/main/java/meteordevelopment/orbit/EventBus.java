package meteordevelopment.orbit;

import lombok.extern.slf4j.Slf4j;
import meteordevelopment.orbit.listeners.IListener;
import meteordevelopment.orbit.listeners.LambdaListener;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

@Slf4j
public class EventBus {

    private final Map<Object, List<IListener>> listenerCache = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<IListener>> staticListenerCache = new ConcurrentHashMap<>();
    private static final IListener[] NO_LISTENERS = new IListener[0];

    private final Map<Class<?>, IListener[]> listenerMap = new ConcurrentHashMap<>();
    private final List<LambdaFactoryInfo> lambdaFactoryInfos = new ArrayList<>();

    public void registerLambdaFactory(String packagePrefix, LambdaListener.Factory factory) {
        synchronized (lambdaFactoryInfos) {
            lambdaFactoryInfos.add(new LambdaFactoryInfo(packagePrefix, factory));
        }
    }

    public <T> T post(T event) {
        IListener[] listeners = listenerMap.get(event.getClass());

        if (listeners != null) {
            for (IListener listener : listeners) {
                callSafely(listener, event);
            }
        }

        return event;
    }

    public <T extends ICancellable> T post(T event) {
        IListener[] listeners = listenerMap.get(event.getClass());

        if (listeners != null) {
            event.setCancelled(false);

            for (IListener listener : listeners) {
                callSafely(listener, event);
                if (event.isCancelled()) {
                    break;
                }
            }
        }

        return event;
    }

    private void callSafely(IListener listener, Object event) {
        try {
            listener.call(event);
        } catch (Throwable throwable) {
            log.error("Event listener failed while handling {}", event.getClass().getName(), throwable);
        }
    }

    public void subscribe(Object object) {
        subscribe(getListeners(object.getClass(), object), false);
    }

    public void subscribe(Class<?> klass) {
        subscribe(getListeners(klass, null), true);
    }

    private void subscribe(List<IListener> listeners, boolean onlyStatic) {
        for (IListener listener : listeners) {
            subscribe(listener, onlyStatic);
        }
    }

    private void subscribe(IListener listener, boolean onlyStatic) {
        if (onlyStatic && !listener.isStatic()) {
            return;
        }
        insert(listener);
    }

    /**
     * Publishes a new dispatch array for the listener's target with the listener inserted before the first existing
     * listener of lower priority. Posting threads keep iterating whichever array they already read.
     *
     * @param listener the listener to add
     */
    private void insert(IListener listener) {
        synchronized (listenerMap) {
            Class<?> target = listener.getTarget();
            IListener[] current = listenerMap.getOrDefault(target, NO_LISTENERS);
            int index = 0;
            while (index < current.length && listener.getPriority() <= current[index].getPriority()) {
                index++;
            }

            IListener[] updated = new IListener[current.length + 1];
            System.arraycopy(current, 0, updated, 0, index);
            updated[index] = listener;
            System.arraycopy(current, index, updated, index + 1, current.length - index);
            listenerMap.put(target, updated);
        }
    }

    private List<IListener> getListeners(Class<?> klass, Object object) {
        Function<Object, List<IListener>> func = o -> {
            List<IListener> listeners = new CopyOnWriteArrayList<>();

            getListeners(listeners, klass, object);

            return listeners;
        };

        if (object == null) {
            return staticListenerCache.computeIfAbsent(klass, func);
        }

        // We need to check if the instances are the same and avoid using .equals() and .hashCode()
        for (Object key : listenerCache.keySet()) {
            if (key == object) {
                return listenerCache.get(object);
            }
        }

        List<IListener> listeners = func.apply(object);
        listenerCache.put(object, listeners);
        return listeners;
    }

    private void getListeners(List<IListener> listeners, Class<?> klass, Object object) {
        for (Method method : klass.getDeclaredMethods()) {
            if (isValid(method)) {
                LambdaListener.Factory factory = getLambdaFactory(klass);
                try {
                    listeners.add(new LambdaListener(factory, klass, object, method));
                } catch (Throwable throwable) {
                    log.error("Skipping event listener {}#{}", klass.getName(), method.getName(), throwable);
                }
            }
        }

        if (klass.getSuperclass() != null) {
            getListeners(listeners, klass.getSuperclass(), object);
        }
    }

    private boolean isValid(Method method) {
        if (!method.isAnnotationPresent(EventHandler.class)) {
            return false;
        }
        if (method.getReturnType() != void.class) {
            return false;
        }
        if (method.getParameterCount() != 1) {
            return false;
        }

        return !method.getParameters()[0].getType().isPrimitive();
    }

    private LambdaListener.Factory getLambdaFactory(Class<?> klass) {
        synchronized (lambdaFactoryInfos) {
            for (LambdaFactoryInfo info : lambdaFactoryInfos) {
                if (klass.getName().startsWith(info.packagePrefix)) {
                    return info.factory;
                }
            }
        }

        throw new NoLambdaFactoryException(klass);
    }

    private record LambdaFactoryInfo(String packagePrefix, LambdaListener.Factory factory) {}
}
