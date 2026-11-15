package cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.hud;

import cc.fascinated.fascinatedutils.common.Colors;
import cc.fascinated.fascinatedutils.common.NumberUtils;
import cc.fascinated.fascinatedutils.common.StringUtils;
import cc.fascinated.fascinatedutils.common.culling.BiomeColors;
import cc.fascinated.fascinatedutils.gui.render.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.HUDPanelBackground;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget.CoordinatesLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CoordinatesHudPanel extends HudPanel {

    private static final float COLUMN_GAP = 4f;
    private static final String[] COMPASS_DIRECTIONS = {"S", "SE", "E", "NE", "N", "NW", "W", "SW"};

    private final CoordinatesWidget coordinatesWidget;
    private final Map<String, String> biomeMiniMessageCache = new HashMap<>();
    private double cachedCoordX = Double.NaN;
    private double cachedCoordY = Double.NaN;
    private double cachedCoordZ = Double.NaN;
    private int cachedDecimals = -1;
    private String cachedBiomeMini;
    private String xPart;
    private String yPart;
    private String zPart;
    private List<String> verticalLines = List.of();

    public CoordinatesHudPanel(CoordinatesWidget coordinatesWidget) {
        super(coordinatesWidget, "coordinates", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.coordinatesWidget = coordinatesWidget;
    }

    /**
     * Rebuilds the coordinate strings only when a coordinate, the precision setting, or the biome changes.
     *
     * @param coordX    block X position shown
     * @param coordY    block Y position shown
     * @param coordZ    block Z position shown
     * @param decimals  decimal places for the coordinates
     * @param biomeMini biome label in MiniMessage form
     */
    private void refreshCoordinateText(double coordX, double coordY, double coordZ, int decimals, String biomeMini) {
        if (coordX == cachedCoordX && coordY == cachedCoordY && coordZ == cachedCoordZ && decimals == cachedDecimals && biomeMini.equals(cachedBiomeMini)) {
            return;
        }
        cachedCoordX = coordX;
        cachedCoordY = coordY;
        cachedCoordZ = coordZ;
        cachedDecimals = decimals;
        cachedBiomeMini = biomeMini;
        xPart = "X: " + NumberUtils.formatNumber(coordX, decimals);
        yPart = "Y: " + NumberUtils.formatNumber(coordY, decimals);
        zPart = "Z: " + NumberUtils.formatNumber(coordZ, decimals);
        verticalLines = List.of(xPart, yPart, zPart, "Biome: " + biomeMini);
    }

    private String biomeColoredMiniMessage(String biomeIdRaw) {
        return biomeMiniMessageCache.computeIfAbsent(biomeIdRaw, CoordinatesHudPanel::buildBiomeColoredMiniMessage);
    }

    private static String buildBiomeColoredMiniMessage(String biomeIdRaw) {
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
        refreshCoordinateText(coordX, coordY, coordZ, decimals, biomeMini);
        if (coordinatesWidget.coordinatesHudLayout() == CoordinatesLayout.HORIZONTAL) {
            final String xPart = this.xPart;
            final String yPart = this.yPart;
            final String zPart = this.zPart;
            float lineHeight = glRenderer.fontHeight();
            float xWidth = glRenderer.measureMiniMessageTextWidth(xPart);
            float yWidth = glRenderer.measureMiniMessageTextWidth(yPart);
            float zWidth = glRenderer.measureMiniMessageTextWidth(zPart);
            float maxFacingWidth = 0f;
            for (String directionLabel : COMPASS_DIRECTIONS) {
                maxFacingWidth = Math.max(maxFacingWidth, glRenderer.measureMiniMessageTextWidth(directionLabel));
            }
            final float facingColumnBandWidth = maxFacingWidth;
            float innerWidth = xWidth + COLUMN_GAP + yWidth + COLUMN_GAP + zWidth + COLUMN_GAP + facingColumnBandWidth;
            float padding = hudHostModule().getPaddingValue();
            float layoutWidth = Math.max(1f, Math.max(getMinWidth(), innerWidth + 2f * padding));
            float layoutHeight = Math.max(1f, lineHeight + 2f * padding);
            commitLayout(layoutWidth, layoutHeight);
            boolean textShadow = hudHostModule().isTextShadowEnabled();
            return () -> {
                hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
                float availableInnerWidth = layoutWidth - 2f * padding;
                float startX = padding + HudAnchorLayout.horizontalOffsetInInnerBand(availableInnerWidth, innerWidth, hudContentHorizontalAlignment());
                glRenderer.drawMiniMessageText(xPart, startX, padding, textShadow);
                glRenderer.drawMiniMessageText(yPart, startX + xWidth + COLUMN_GAP, padding, textShadow);
                glRenderer.drawMiniMessageText(zPart, startX + xWidth + COLUMN_GAP + yWidth + COLUMN_GAP, padding, textShadow);
                float facingColumnX = startX + xWidth + COLUMN_GAP + yWidth + COLUMN_GAP + zWidth + COLUMN_GAP;
                float currentFacingHeadingWidth = glRenderer.measureMiniMessageTextWidth(facingKey);
                glRenderer.drawMiniMessageText(facingKey, facingColumnX + (facingColumnBandWidth - currentFacingHeadingWidth), padding, textShadow);
            };
        }
        List<String> leftLines = verticalLines;
        float lineHeight = glRenderer.fontHeight();
        float leftColumnWidth = 0f;
        for (String line : leftLines) {
            leftColumnWidth = Math.max(leftColumnWidth, glRenderer.measureMiniMessageTextWidth(line));
        }
        int lineCount = leftLines.size();
        float facingWidth = 0f;
        for (String directionLabel : COMPASS_DIRECTIONS) {
            facingWidth = Math.max(facingWidth, glRenderer.measureMiniMessageTextWidth(directionLabel));
        }
        final float leftColumnCommitWidth = leftColumnWidth;
        final float facingCommitWidth = facingWidth;
        float innerHeight = lineCount * lineHeight + Math.max(0, lineCount - 1) * HUDPanelBackground.LINE_GAP_PX;
        float innerWidth = leftColumnWidth + COLUMN_GAP + facingWidth;
        float padding = hudHostModule().getPaddingValue();
        float layoutWidth = Math.max(1f, Math.max(getMinWidth(), innerWidth + 2f * padding));
        float layoutHeight = Math.max(1f, innerHeight + 2f * padding);
        commitLayout(layoutWidth, layoutHeight);

        boolean textShadow = hudHostModule().isTextShadowEnabled();
        return () -> {
            hudHostModule().drawHUDPanelBackground(glRenderer, layoutWidth, layoutHeight, editorMode);
            float availableInnerWidth = layoutWidth - 2f * padding;
            float blockStartX = padding + HudAnchorLayout.horizontalOffsetInInnerBand(availableInnerWidth, innerWidth, hudContentHorizontalAlignment());
            float cursorY = padding;
            for (int index = 0; index < lineCount; index++) {
                glRenderer.drawMiniMessageText(leftLines.get(index), blockStartX, cursorY, textShadow);
                cursorY += lineHeight;
                if (index < lineCount - 1) {
                    cursorY += HUDPanelBackground.LINE_GAP_PX;
                }
            }
            float facingColumnStartX = blockStartX + leftColumnCommitWidth + COLUMN_GAP;
            float currentFacingHeadingWidth = glRenderer.measureMiniMessageTextWidth(facingKey);
            float facingX = facingColumnStartX + (facingCommitWidth - currentFacingHeadingWidth);
            glRenderer.drawMiniMessageText(facingKey, facingX, padding, textShadow);
        };
    }
}
