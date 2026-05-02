package cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.common.culling.BiomeColors;
import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget.CoordinatesLayout;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CoordinatesHudPanel extends HudPanel {

    private static final float COLUMN_GAP = 4f;
    private static final float LINE_GAP_PX = 1f;
    private static final String[] COMPASS_DIRECTIONS = {"S", "SE", "E", "NE", "N", "NW", "W", "SW"};

    private final CoordinatesWidget coordinatesWidget;

    public CoordinatesHudPanel(CoordinatesWidget coordinatesWidget) {
        super(coordinatesWidget, "coordinates", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.coordinatesWidget = coordinatesWidget;
    }

    @Override
    protected @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode) {
        return null;
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        final double coordX;
        final double coordY;
        final double coordZ;
        final String biomeMini;
        final String facingKey;
        if (player != null) {
            coordX = player.getX();
            coordY = player.getY();
            coordZ = player.getZ();
            biomeMini = biomeColoredMiniMessage(player.level().getBiome(player.blockPosition()).getRegisteredName());
            facingKey = compassFromLook(player.getViewVector(client.getDeltaTracker().getGameTimeDeltaPartialTick(false)));
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
        int decimals = Math.round(coordinatesWidget.coordinatesHudBlockDecimals());
        if (coordinatesWidget.coordinatesHudLayout() == CoordinatesLayout.HORIZONTAL) {
            String xPart = "X: " + NumberUtils.formatNumber(coordX, decimals);
            String yPart = "Y: " + NumberUtils.formatNumber(coordY, decimals);
            String zPart = "Z: " + NumberUtils.formatNumber(coordZ, decimals);
            float lineHeight = glRenderer.getFontHeight();
            float xWidth = glRenderer.measureMiniMessageTextWidth(xPart);
            float yWidth = glRenderer.measureMiniMessageTextWidth(yPart);
            float zWidth = glRenderer.measureMiniMessageTextWidth(zPart);
            float maxFacingWidth = 0f;
            for (String directionLabel : COMPASS_DIRECTIONS) {
                maxFacingWidth = Math.max(maxFacingWidth, glRenderer.measureMiniMessageTextWidth(directionLabel));
            }
            float innerWidth = xWidth + COLUMN_GAP + yWidth + COLUMN_GAP + zWidth + COLUMN_GAP + maxFacingWidth;
            float padding = hudHostModule().getPadding();
            float layoutWidth = Math.max(1f, Math.max(getMinWidth(), innerWidth + 2f * padding));
            float layoutHeight = Math.max(1f, lineHeight + 2f * padding);
            getHudState().setLastLayoutWidth(layoutWidth);
            getHudState().setLastLayoutHeight(layoutHeight);
            getHudState().setCommittedLayoutWidth(layoutWidth);
            getHudState().setCommittedLayoutHeight(layoutHeight);
            boolean textShadow = hudHostModule().isTextShadowEnabled();
            float capturedXWidth = xWidth;
            float capturedYWidth = yWidth;
            float capturedZWidth = zWidth;
            float capturedMaxFacingWidth = maxFacingWidth;
            return () -> {
                hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
                float availableInnerWidth = layoutWidth - 2f * padding;
                float startX = padding + HudAnchorLayout.horizontalOffsetInInnerBand(availableInnerWidth, innerWidth, hudContentHorizontalAlignment());
                float lineY = padding;
                glRenderer.drawMiniMessageText(xPart, startX, lineY, textShadow);
                glRenderer.drawMiniMessageText(yPart, startX + capturedXWidth + COLUMN_GAP, lineY, textShadow);
                glRenderer.drawMiniMessageText(zPart, startX + capturedXWidth + COLUMN_GAP + capturedYWidth + COLUMN_GAP, lineY, textShadow);
                float facingColumnX = startX + capturedXWidth + COLUMN_GAP + capturedYWidth + COLUMN_GAP + capturedZWidth + COLUMN_GAP;
                float currentFacingHeadingWidth = glRenderer.measureMiniMessageTextWidth(facingKey);
                glRenderer.drawMiniMessageText(facingKey, facingColumnX + (capturedMaxFacingWidth - currentFacingHeadingWidth), lineY, textShadow);
            };
        }
        List<String> leftLines = List.of("X: " + NumberUtils.formatNumber(coordX, decimals), "Y: " + NumberUtils.formatNumber(coordY, decimals), "Z: " + NumberUtils.formatNumber(coordZ, decimals), "Biome: " + biomeMini);
        float lineHeight = glRenderer.getFontHeight();
        float leftColumnWidth = 0f;
        for (String line : leftLines) {
            leftColumnWidth = Math.max(leftColumnWidth, glRenderer.measureMiniMessageTextWidth(line));
        }
        int lineCount = leftLines.size();
        float facingWidth = 0f;
        for (String directionLabel : COMPASS_DIRECTIONS) {
            facingWidth = Math.max(facingWidth, glRenderer.measureMiniMessageTextWidth(directionLabel));
        }
        float innerHeight = lineCount * lineHeight + Math.max(0, lineCount - 1) * LINE_GAP_PX;
        float innerWidth = leftColumnWidth + COLUMN_GAP + facingWidth;
        float padding = hudHostModule().getPadding();
        float layoutWidth = Math.max(1f, Math.max(getMinWidth(), innerWidth + 2f * padding));
        float layoutHeight = Math.max(1f, innerHeight + 2f * padding);
        getHudState().setLastLayoutWidth(layoutWidth);
        getHudState().setLastLayoutHeight(layoutHeight);
        getHudState().setCommittedLayoutWidth(layoutWidth);
        getHudState().setCommittedLayoutHeight(layoutHeight);

        float capturedLeftColumnWidth = leftColumnWidth;
        float capturedFacingWidth = facingWidth;
        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
            float availableInnerWidth = layoutWidth - 2f * padding;
            float blockStartX = padding + HudAnchorLayout.horizontalOffsetInInnerBand(availableInnerWidth, innerWidth, hudContentHorizontalAlignment());
            float innerTop = padding;
            float cursorY = innerTop;
            for (int index = 0; index < lineCount; index++) {
                glRenderer.drawMiniMessageText(leftLines.get(index), blockStartX, cursorY, textShadow);
                cursorY += lineHeight;
                if (index < lineCount - 1) {
                    cursorY += LINE_GAP_PX;
                }
            }
            float facingColumnStartX = blockStartX + capturedLeftColumnWidth + COLUMN_GAP;
            float currentFacingHeadingWidth = glRenderer.measureMiniMessageTextWidth(facingKey);
            float facingX = facingColumnStartX + (capturedFacingWidth - currentFacingHeadingWidth);
            glRenderer.drawMiniMessageText(facingKey, facingX, innerTop, textShadow);
        };
    }

    private static String biomeColoredMiniMessage(String biomeIdRaw) {
        Identifier biomeId = Identifier.tryParse(biomeIdRaw);
        String path = biomeId == null ? biomeIdRaw : biomeId.getPath();
        String label = Arrays.stream(path.replace("_", " ").split(" ")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        int biomeColorArgb = BiomeColors.colorForBiomeId(Identifier.tryParse(biomeIdRaw));
        return "<color:" + Colors.rgbHex(biomeColorArgb) + ">" + label + "</color>";
    }

    private static String compassFromLook(Vec3 look) {
        double horizontalX = look.x;
        double horizontalZ = look.z;
        if (Math.hypot(horizontalX, horizontalZ) < 1e-5d) {
            return "—";
        }
        double degrees = Math.toDegrees(Math.atan2(horizontalX, horizontalZ));
        if (degrees < 0d) {
            degrees += 360d;
        }
        int sector = (int) Math.floor((degrees + 22.5d) / 45d) & 7;
        return COMPASS_DIRECTIONS[sector];
    }
}
