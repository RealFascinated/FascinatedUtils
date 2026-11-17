package cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.hud;

import cc.fascinated.fascinatedutils.common.color.Colors;
import cc.fascinated.fascinatedutils.common.util.NumberUtils;
import cc.fascinated.fascinatedutils.common.util.StringUtils;
import cc.fascinated.fascinatedutils.common.culling.BiomeColors;
import cc.fascinated.fascinatedutils.gui.core.UiNode;
import cc.fascinated.fascinatedutils.gui.node.display.RichTextNode;
import cc.fascinated.fascinatedutils.gui.node.layout.ColumnNode;
import cc.fascinated.fascinatedutils.gui.node.layout.RowNode;
import cc.fascinated.fascinatedutils.gui.node.layout.WidestNode;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudComposeContext;
import cc.fascinated.fascinatedutils.systems.hud.HudHostModule;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorLayout;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget;
import cc.fascinated.fascinatedutils.systems.modules.impl.coordinates.CoordinatesWidget.CoordinatesLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CoordinatesHudPanel extends HudPanel {

    private static final int COLUMN_GAP = 4;
    private static final String[] COMPASS_DIRECTIONS = {"S", "SE", "E", "NE", "N", "NW", "W", "SW"};
    private static final int PREVIEW_COMPASS_INDEX = 2;
    private static final String COMPASS_TRANSLATION_PREFIX = "alumite.module.coordinates.compass.";
    private static final String PREVIEW_BIOME_ID = "minecraft:river";

    private final CoordinatesWidget coordinatesWidget;

    public CoordinatesHudPanel(CoordinatesWidget coordinatesWidget) {
        super(coordinatesWidget, "coordinates", HudHostModule.UTILITY_WIDGET_MIN_WIDTH);
        this.coordinatesWidget = coordinatesWidget;
    }

    private static String buildBiomeColoredMiniMessage(String biomeIdRaw) {
        Identifier biomeId = Identifier.tryParse(biomeIdRaw);
        String path = biomeId == null ? biomeIdRaw : biomeId.getPath();
        String label = Arrays.stream(path.replace("_", " ").split(" ")).map(StringUtils::capitalize).collect(Collectors.joining(" "));
        int biomeColorArgb = BiomeColors.colorForBiomeId(biomeId);
        return "<color:" + Colors.rgbHex(biomeColorArgb) + ">" + escapeMiniMessage(label) + "</color>";
    }

    private static int compassIndexFromYaw(float yawDegrees) {
        // Yaw 0 faces south and grows clockwise (towards west); the compass list runs south, south-east, east, ...
        double heading = -yawDegrees % 360d;
        if (heading < 0d) {
            heading += 360d;
        }
        return (int) Math.floor((heading + 22.5d) / 45d) & 7;
    }

    private static String[] translatedCompassLabels() {
        String[] labels = new String[COMPASS_DIRECTIONS.length];
        for (int index = 0; index < COMPASS_DIRECTIONS.length; index++) {
            labels[index] = I18n.get(COMPASS_TRANSLATION_PREFIX + COMPASS_DIRECTIONS[index].toLowerCase(Locale.ROOT));
        }
        return labels;
    }

    @Override
    protected @Nullable UiNode compose(HudComposeContext context) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        double coordX;
        double coordY;
        double coordZ;
        String biomeId;
        int facingIndex;
        if (player != null) {
            coordX = player.getX();
            coordY = player.getY();
            coordZ = player.getZ();
            biomeId = player.level().getBiome(player.blockPosition()).getRegisteredName();
            facingIndex = compassIndexFromYaw(player.getYRot());
        }
        else if (context.editorMode()) {
            coordX = 12.375;
            coordY = 64.0625;
            coordZ = -8.254;
            biomeId = PREVIEW_BIOME_ID;
            facingIndex = PREVIEW_COMPASS_INDEX;
        }
        else {
            return null;
        }
        int decimals = Math.round(coordinatesWidget.coordinatesHudBlockDecimals());
        RichTextNode xLine = plainLine(context, I18n.get("alumite.module.coordinates.label.x") + NumberUtils.formatNumber(coordX, decimals));
        RichTextNode yLine = plainLine(context, I18n.get("alumite.module.coordinates.label.y") + NumberUtils.formatNumber(coordY, decimals));
        RichTextNode zLine = plainLine(context, I18n.get("alumite.module.coordinates.label.z") + NumberUtils.formatNumber(coordZ, decimals));
        String[] compassLabels = translatedCompassLabels();
        // The compass column is as wide as the widest direction so the panel does not resize while turning.
        List<RichTextNode> compassCandidates = new ArrayList<>(compassLabels.length);
        for (String compassLabel : compassLabels) {
            compassCandidates.add(plainLine(context, compassLabel));
        }
        WidestNode compassColumn = new WidestNode(compassCandidates);
        compassColumn.addChild(plainLine(context, compassLabels[facingIndex]).alignX(1f));

        RowNode row = new RowNode(COLUMN_GAP).alignX(context.contentAlignment());
        if (coordinatesWidget.coordinatesHudLayout() == CoordinatesLayout.HORIZONTAL) {
            row.addChild(xLine);
            row.addChild(yLine);
            row.addChild(zLine);
        }
        else {
            ColumnNode lines = new ColumnNode(Math.round(HudAnchorLayout.LINE_GAP_PX));
            lines.addChild(xLine);
            lines.addChild(yLine);
            lines.addChild(zLine);
            lines.addChild(context.text(I18n.get("alumite.module.coordinates.label.biome") + buildBiomeColoredMiniMessage(biomeId)));
            row.addChild(lines);
        }
        row.addChild(compassColumn);
        return row;
    }

    private static RichTextNode plainLine(HudComposeContext context, String text) {
        return RichTextNode.component(Component.literal(text), context.textShadow());
    }
}
