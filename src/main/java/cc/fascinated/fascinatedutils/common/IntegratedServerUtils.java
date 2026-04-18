package cc.fascinated.fascinatedutils.common;

import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

@UtilityClass
public class IntegratedServerUtils {

    /**
     * Best-effort average server tick time in milliseconds for the running integrated server, or {@link Float#NaN}
     * when not in singleplayer or when no usable metric is exposed.
     *
     * @param minecraftClient client whose integrated server is queried
     * @return positive MSPT when known, otherwise not-a-number
     */
    public static float sampleAverageMspt(Minecraft minecraftClient) {
        if (!minecraftClient.hasSingleplayerServer()) {
            return Float.NaN;
        }
        Object integratedServer = minecraftClient.getSingleplayerServer();
        if (integratedServer == null) {
            return Float.NaN;
        }
        float fromAverageTickTime = toPositiveMspt(invokeNumberNoArgs(integratedServer, "getAverageTickTime"), 1f);
        if (Float.isFinite(fromAverageTickTime)) {
            return fromAverageTickTime;
        }
        float fromAverageNanosPerTick = toPositiveMspt(invokeNumberNoArgs(integratedServer, "getAverageNanosPerTick"), 1_000_000f);
        if (Float.isFinite(fromAverageNanosPerTick)) {
            return fromAverageNanosPerTick;
        }
        float fromTickTimes = averageTickTimesMspt(invokeNoArgs(integratedServer, "getTickTimes"));
        if (Float.isFinite(fromTickTimes)) {
            return fromTickTimes;
        }
        return averageTickTimesMspt(invokeNoArgs(integratedServer, "getTickTimesNanos"));
    }

    private static Object invokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Number invokeNumberNoArgs(Object target, String methodName) {
        Object value = invokeNoArgs(target, methodName);
        if (value instanceof Number numberValue) {
            return numberValue;
        }
        return null;
    }

    private static float toPositiveMspt(Number rawValue, float divisor) {
        if (rawValue == null || divisor <= 0f) {
            return Float.NaN;
        }
        float normalized = (float) (rawValue.doubleValue() / divisor);
        if (!Float.isFinite(normalized) || normalized <= 0f) {
            return Float.NaN;
        }
        return normalized;
    }

    private static float averageTickTimesMspt(Object tickTimesArrayObject) {
        if (tickTimesArrayObject == null || !tickTimesArrayObject.getClass().isArray()) {
            return Float.NaN;
        }
        int length = Array.getLength(tickTimesArrayObject);
        if (length == 0) {
            return Float.NaN;
        }
        double nanosSum = 0d;
        int sampleCount = 0;
        for (int sampleIndex = 0; sampleIndex < length; sampleIndex++) {
            Object sampleValue = Array.get(tickTimesArrayObject, sampleIndex);
            if (!(sampleValue instanceof Number sampleNumber)) {
                continue;
            }
            double sampleNanos = sampleNumber.doubleValue();
            if (!Double.isFinite(sampleNanos) || sampleNanos <= 0d) {
                continue;
            }
            nanosSum += sampleNanos;
            sampleCount++;
        }
        if (sampleCount == 0) {
            return Float.NaN;
        }
        return (float) ((nanosSum / sampleCount) / 1_000_000d);
    }
}
