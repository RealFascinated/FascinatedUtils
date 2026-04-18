package cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.feature;

import cc.fascinated.fascinatedutils.FascinatedUtils;
import cc.fascinated.fascinatedutils.client.Client;
import cc.fascinated.fascinatedutils.common.PatternHandler;
import cc.fascinated.fascinatedutils.common.PlaceholderAPI;
import cc.fascinated.fascinatedutils.common.PlayerUtils;
import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.chat.ChatMessageEvent;
import cc.fascinated.fascinatedutils.systems.config.YMLConfigFile;
import cc.fascinated.fascinatedutils.systems.modules.Feature;
import cc.fascinated.fascinatedutils.systems.modules.impl.hypixel.HypixelModule;
import meteordevelopment.orbit.EventHandler;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class AutoGG extends Feature<HypixelModule> {

    private static final String AUTOGG_TRIGGERS_RESOURCE = "/modules/hypixel/autogg.yml";
    private static final List<String> PRIMARY_ANTI_GG_STRINGS = List.of("gg", "GG", "gf", "Good Game", "Good Fight", "Good Round! :D");
    private static final List<String> SECONDARY_ANTI_GG_STRINGS = List.of("Have a good day!", "<3", "AutoGG By Sk1er!", "gf", "Good Fight", "Good Round", ":D", "Well played!", "wp");
    private static final List<AutoGGTrigger> TRIGGERS = loadTriggersFromResource();
    private long lastAutoGG;

    public AutoGG(HypixelModule module) {
        super(module);
        loadTriggersFromResource();

        Set<String> antiGGStrings = new LinkedHashSet<>();
        antiGGStrings.addAll(PRIMARY_ANTI_GG_STRINGS);
        antiGGStrings.addAll(SECONDARY_ANTI_GG_STRINGS);
        PlaceholderAPI.INSTANCE.registerPlaceHolder("antigg_strings", String.join("|", antiGGStrings));

        FascinatedEventBus.INSTANCE.subscribe(this);
    }

    private static List<AutoGGTrigger> loadTriggersFromResource() {
        YMLConfigFile configFile = new YMLConfigFile(AUTOGG_TRIGGERS_RESOURCE);
        Map<String, Object> configRoot = configFile.loadAsMap();
        Object rawTriggers = configRoot.get("triggers");
        if (!(rawTriggers instanceof List<?> triggersList)) {
            Client.LOG.warn("Missing or invalid AutoGG trigger resource {}, triggers disabled.", AUTOGG_TRIGGERS_RESOURCE);
            return Collections.emptyList();
        }
        List<AutoGGTrigger> loadedTriggers = new ArrayList<>();
        try {
            for (Object triggerEntry : triggersList) {
                if (!(triggerEntry instanceof Map<?, ?> triggerMap)) {
                    continue;
                }
                Object typeValue = triggerMap.get("type");
                Object patternValue = triggerMap.get("pattern");
                if (!(typeValue instanceof String typeString) || !(patternValue instanceof String patternString)) {
                    continue;
                }
                if (patternString.isBlank()) {
                    continue;
                }
                AutoGGType triggerType = AutoGGType.valueOf(typeString.trim().toUpperCase());
                loadedTriggers.add(new AutoGGTrigger(triggerType, patternString));
            }
        } catch (Exception exception) {
            Client.LOG.warn("Failed to load AutoGG triggers from {}, triggers disabled: {}", AUTOGG_TRIGGERS_RESOURCE, exception.toString());
            return Collections.emptyList();
        }
        if (loadedTriggers.isEmpty()) {
            Client.LOG.warn("AutoGG trigger resource {} has no valid triggers, triggers disabled.", AUTOGG_TRIGGERS_RESOURCE);
            return Collections.emptyList();
        }
        return List.copyOf(loadedTriggers);
    }

    @EventHandler
    public void onChatMessage(ChatMessageEvent event) {
        if (getModule().getAutoGG().isDisabled() || !getModule().isOnHypixel() || System.currentTimeMillis() - lastAutoGG < 10_000) {
            return;
        }

        String message = StringUtils.stripColors(event.getRawMessage());
        for (AutoGGTrigger trigger : TRIGGERS) {
            switch (trigger.type) {
                case ANTI_GG, ANTI_KARMA -> {
                    if (PatternHandler.INSTANCE.getPattern(trigger.pattern).matcher(message).matches()) {
                        event.setCancelled(true);
                        return;
                    }
                }
                case NORMAL, CASUAL -> {
                    if (PatternHandler.INSTANCE.getPattern(trigger.pattern).matcher(message).matches()) {
                        lastAutoGG = System.currentTimeMillis();
                        FascinatedUtils.SCHEDULED_POOL.schedule(() -> PlayerUtils.runCommand("ac gg"), 1, TimeUnit.SECONDS);
                        return;
                    }
                }
            }
        }
    }

    private enum AutoGGType {
        NORMAL, CASUAL, ANTI_GG, ANTI_KARMA
    }

    private record AutoGGTrigger(AutoGGType type, String pattern) {}
}
