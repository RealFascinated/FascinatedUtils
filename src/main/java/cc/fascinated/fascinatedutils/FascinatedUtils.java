package cc.fascinated.fascinatedutils;

import net.fabricmc.api.ModInitializer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class FascinatedUtils implements ModInitializer {
    public static final String MOD_ID = "fascinatedutils";

    public static final ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(4);

    @Override
    public void onInitialize() {
    }
}
