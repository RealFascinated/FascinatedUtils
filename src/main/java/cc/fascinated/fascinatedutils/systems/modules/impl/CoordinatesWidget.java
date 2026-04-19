package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.common.ColorUtils;
import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.common.culling.BiomeColors;
import cc.fascinated.fascinatedutils.common.setting.impl.SliderSetting;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.gui.theme.UITheme;
import cc.fascinated.fascinatedutils.systems.hud.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudModule;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoordinatesWidget extends HudModule {
    private static final float COLUMN_GAP = 10f;
    private static final float HORIZONTAL_PADDING = UITheme.PADDING_SM;
    private static final float VERTICAL_PADDING = UITheme.PADDING_SM;
    private static final float LINE_GAP_PX = 1f;
    private static final String[] COMPASS_CARDINALS = {"S", "E", "N", "W"};
    private final SliderSetting blockPrecision = SliderSetting.builder().id("block_precision")

            .defaultValue(0f).minValue(0f).maxValue(3f).step(1f).categoryDisplayKey(APPEARANCE_CATEGORY_DISPLAY_KEY).build();

    public CoordinatesWidget() {
        super("coordinates", "Coordinates", 56f);
        addSetting(blockPrecision);
    }

    private static String biomeColoredMiniMessage(String biomeIdRaw) {
        Identifier biomeId = Identifier.tryParse(biomeIdRaw);
        String path = biomeId == null ? biomeIdRaw : biomeId.getPath();
        String label = Arrays.stream(path.replace("_", " ").split(" ")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        int biomeColorArgb = BiomeColors.colorForBiomeId(Identifier.tryParse(biomeIdRaw));
        return "<color:" + ColorUtils.rgbHex(biomeColorArgb) + ">" + label + "</color>";
    }

    private static String compass4FromLook(Vec3 look) {
        double horizontalX = look.x;
        double horizontalZ = look.z;
        if (Math.hypot(horizontalX, horizontalZ) < 1e-5d) {
            return "—";
        }
        double degrees = Math.toDegrees(Math.atan2(horizontalX, horizontalZ));
        if (degrees < 0d) {
            degrees += 360d;
        }
        int sector = (int) Math.floor((degrees + 45d) / 90d) & 3;
        return COMPASS_CARDINALS[sector];
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        final double coordX, coordY, coordZ;
        final String biomeMini, facingKey;
        if (player != null) {
            coordX = player.getX();
            coordY = player.getY();
            coordZ = player.getZ();
            biomeMini = biomeColoredMiniMessage(player.level().getBiome(player.blockPosition()).getRegisteredName());
            facingKey = compass4FromLook(player.getViewVector(client.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
        }
        else if (editorMode) {
            coordX = 12.375;
            coordY = 64.0625;
            coordZ = -8.254;
            biomeMini = biomeColoredMiniMessage("minecraft:river");
            facingKey = "E";
        }
        else {
            return null;
        }
        int decimals = Math.round(blockPrecision.getValue().floatValue());
        List<String> leftLines = List.of("X: " + NumberUtils.formatNumber(coordX, decimals), "Y: " + NumberUtils.formatNumber(coordY, decimals), "Z: " + NumberUtils.formatNumber(coordZ, decimals), "Biome: " + biomeMini);
        float lineHeight = glRenderer.getFontHeight();
        float leftColumnWidth = 0f;
        for (String line : leftLines) {
            leftColumnWidth = Math.max(leftColumnWidth, glRenderer.measureMiniMessageTextWidth(line));
        }
        int lineCount = leftLines.size();
        float facingWidth = glRenderer.measureMiniMessageTextWidth(facingKey);
        float innerHeight = lineCount * lineHeight + Math.max(0, lineCount - 1) * LINE_GAP_PX;
        float innerWidth = leftColumnWidth + COLUMN_GAP + facingWidth;
        float layoutWidth = Math.max(1f, Math.max(getMinWidth(), innerWidth + 2f * HORIZONTAL_PADDING));
        float layoutHeight = Math.max(1f, innerHeight + 2f * VERTICAL_PADDING);
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        float capturedLeftColumnWidth = leftColumnWidth;
        return () -> {
            drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight);
            float pad = HORIZONTAL_PADDING;
            float availableInnerWidth = layoutWidth - 2f * pad;
            float blockStartX = pad + HudAnchorLayout.horizontalOffsetInInnerBand(availableInnerWidth, innerWidth, hudContentHorizontalAlignment());
            float innerTop = VERTICAL_PADDING;
            float cursorY = innerTop;
            for (int index = 0; index < lineCount; index++) {
                glRenderer.drawMiniMessageText(leftLines.get(index), blockStartX, cursorY, false);
                cursorY += lineHeight;
                if (index < lineCount - 1) {
                    cursorY += LINE_GAP_PX;
                }
            }
            float facingX = blockStartX + capturedLeftColumnWidth + COLUMN_GAP;
            glRenderer.drawMiniMessageText(facingKey, facingX, innerTop, false);
        };
    }

    @Override
    protected HudContent produceContent(float deltaSeconds, boolean editorMode) {
        return null;
    }
}
