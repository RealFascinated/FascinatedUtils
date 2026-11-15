package cc.fascinated.fascinatedutils.debug.perf.scene.impl;

import cc.fascinated.fascinatedutils.debug.perf.scene.PerfScene;
import cc.fascinated.fascinatedutils.debug.perf.scene.PerfSceneContext;

import java.util.Locale;

/**
 * A busy multiplayer server in one scene: block updates, scoreboard changes, chat, action bar, sounds, particles, and
 * armor stands that are spawned, killed and turned every tick. Commands go straight to the connection rather than the
 * context's queue, which only sends four per tick and would cap the burst.
 *
 * <p>Estimated at roughly 120 server-to-client packets per tick for one player. The estimate is not measured.
 */
public class PacketFloodPerfScene extends PerfScene {
    public static final String ID = "packet_flood";

    private static final String TAG = "perf_flood";
    private static final String OBJECTIVE = "perf_flood";
    private static final String SCORE_HOLDER_PREFIX = "perf_flood_score_";
    private static final String ARMOR_STAND_NBT = "{Invisible:1b,Marker:1b,NoGravity:1b,Silent:1b,Tags:[\"%s\",\"%s\"]}";
    private static final int ARMOR_STAND_COUNT = 24;
    private static final int BLOCK_COUNT = 48;
    private static final int SCORE_HOLDER_COUNT = 24;
    private static final int CHAT_LINES_PER_TICK = 8;
    private static final int SOUNDS_PER_TICK = 2;
    private static final int PARTICLES_PER_TICK = 2;
    private static final int AREA_START_X = -12;
    private static final int ARMOR_STAND_Z = 10;
    private static final int BLOCK_Z = 12;
    private static final int BLOCK_HEIGHT = 2;
    private static final int TURN_DEGREES_PER_TICK = 15;
    private static final int FULL_TURN_DEGREES = 360;

    private int timeAddedTicks;

    public PacketFloodPerfScene() {
        super(ID, "Blocks toggled, scores changed, chat, action bar, sounds, particles, and armor stands spawned, killed and turned every tick");
    }

    @Override
    public void setup(PerfSceneContext context) {
        timeAddedTicks = 0;
        send(context, "scoreboard objectives add " + OBJECTIVE + " dummy \"Packet Flood\"");
        send(context, "scoreboard objectives setdisplay sidebar " + OBJECTIVE);
        for (int index = 0; index < ARMOR_STAND_COUNT; index++) {
            send(context, atColumn(columnX(index), ARMOR_STAND_Z, "summon armor_stand ~ ~ ~ " + armorStandNbt(index)));
        }
    }

    @Override
    public void onTick(PerfSceneContext context, int tick) {
        // Even ticks place stone and odd ticks clear it, so every block in the row sends an update each tick.
        String blockState = tick % 2 == 0 ? "stone" : "air";
        for (int index = 0; index < BLOCK_COUNT; index++) {
            send(context, atColumn(columnX(index), BLOCK_Z, "setblock ~ ~" + BLOCK_HEIGHT + " ~ " + blockState));
        }

        int slot = tick % ARMOR_STAND_COUNT;
        send(context, atColumn(columnX(slot), ARMOR_STAND_Z, "kill @e[tag=" + slotTag(slot) + "]"));
        send(context, atColumn(columnX(slot), ARMOR_STAND_Z, "summon armor_stand ~ ~ ~ " + armorStandNbt(slot)));
        int yaw = (tick * TURN_DEGREES_PER_TICK) % FULL_TURN_DEGREES;
        send(context, "execute as @e[tag=" + TAG + "] run tp @s ~ ~ ~ " + yaw + " 0");

        for (int index = 0; index < SCORE_HOLDER_COUNT; index++) {
            send(context, "scoreboard players add " + scoreHolder(index) + " " + OBJECTIVE + " 1");
        }

        for (int line = 0; line < CHAT_LINES_PER_TICK; line++) {
            send(context, "tellraw @a [{\"text\":\"perf \",\"color\":\"gold\",\"bold\":true},{\"text\":\"flood " + tick + "." + line + "\",\"color\":\"aqua\"}]");
        }
        send(context, "title @a actionbar {\"text\":\"Packet flood tick " + tick + "\",\"color\":\"yellow\"}");
        for (int sound = 0; sound < SOUNDS_PER_TICK; sound++) {
            float pitch = 0.5F + 0.1F * ((tick + sound) % 8);
            send(context, "playsound minecraft:block.note_block.pling master @s ~ ~ ~ 0.5 " + String.format(Locale.ROOT, "%.2f", pitch));
        }
        for (int particle = 0; particle < PARTICLES_PER_TICK; particle++) {
            send(context, atColumn(columnX(particle * 4), ARMOR_STAND_Z,"particle minecraft:end_rod ~ ~3 ~ 4 1 4 0.02 15 force"));
        }
        send(context, "time add 1");
        timeAddedTicks++;
    }

    @Override
    public void teardown(PerfSceneContext context) {
        send(context, "kill @e[tag=" + TAG + "]");
        send(context, "scoreboard objectives remove " + OBJECTIVE);
        for (int index = 0; index < BLOCK_COUNT; index++) {
            send(context, atColumn(columnX(index), BLOCK_Z, "setblock ~ ~" + BLOCK_HEIGHT + " ~ air"));
        }
        send(context, "title @a clear");
        send(context, "title @a actionbar {\"text\":\"\"}");
        if (timeAddedTicks > 0) {
            send(context, "time add -" + timeAddedTicks);
            timeAddedTicks = 0;
        }
    }

    private static void send(PerfSceneContext context, String command) {
        context.getMinecraft().getConnection().sendCommand(command);
    }

    private static String atColumn(int offsetX, int offsetZ, String command) {
        return String.format(Locale.ROOT, "execute positioned %d 0 %d positioned over world_surface run %s", offsetX, offsetZ, command);
    }

    private static String armorStandNbt(int slot) {
        return String.format(Locale.ROOT, ARMOR_STAND_NBT, TAG, slotTag(slot));
    }

    private static String slotTag(int slot) {
        return String.format(Locale.ROOT, "%s_%02d", TAG, slot);
    }

    private static String scoreHolder(int index) {
        return String.format(Locale.ROOT, "%s%02d", SCORE_HOLDER_PREFIX, index);
    }

    private static int columnX(int index) {
        return AREA_START_X + index;
    }
}
