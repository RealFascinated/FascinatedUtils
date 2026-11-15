package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.coverage.PerfCoverage;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;

import java.util.List;

/**
 * Five styled chat lines every tick, so chat message handling and styling run continuously.
 */
public class ChatFloodPerfScene extends PerfScene {
    public static final String ID = "chat_flood";
    private static final int LINES_PER_TICK = 4;

    private int lineNumber;

    public ChatFloodPerfScene() {
        super(ID, "Five styled chat lines every tick");
    }

    @Override
    public void setup(PerfSceneContext context) {
        lineNumber = 0;
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        for (int line = 0; line < LINES_PER_TICK; line++) {
            lineNumber++;
            context.command("tellraw @s [{\"text\":\"perf \",\"color\":\"gold\",\"bold\":true},{\"text\":\"flood line " + lineNumber + "\",\"color\":\"aqua\"}]");
        }
    }

    /**
     * Drops the flood lines still queued, so the teardown does not wait while the backlog drains.
     *
     * @param context the running scene's helpers
     */
    @Override
    public void teardown(PerfSceneContext context) {
        context.getPendingCommands().clear();
    }

    @Override
    public List<String> covers() {
        return List.of(PerfCoverage.mixin("ChatComponentMixin"));
    }
}
