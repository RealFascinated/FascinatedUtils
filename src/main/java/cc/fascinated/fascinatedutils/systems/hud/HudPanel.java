package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.gui.renderer.GuiRenderer;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.anchor.HudAnchorContentAlignment;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContent;
import cc.fascinated.fascinatedutils.systems.hud.content.HudContentRenderer;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

public abstract class HudPanel implements HudRenderablePanel {
    private final HudHostModule host;
    @Getter
    private final String id;
    private final float minWidth;
    @Getter
    private final ModuleHudState hudState = new ModuleHudState();

    protected HudPanel(HudHostModule hostModule, String panelId, float minWidth) {
        this.host = hostModule;
        this.id = panelId;
        this.minWidth = minWidth;
        HudDefaults defaults = hostModule.getHudDefaults();
        hudState.setHudAnchor(defaults.defaultAnchor());
        hudState.setAnchorOffsetX(defaults.defaultXOffset());
        hudState.setAnchorOffsetY(defaults.defaultYOffset());
        hudState.setPositionX(defaults.defaultXOffset());
        hudState.setPositionY(defaults.defaultYOffset());
    }

    @Override
    public HudHostModule hudHostModule() {
        return host;
    }

    @Override
    public String getName() {
        return host.getDisplayName();
    }

    @Override
    public float getMinWidth() {
        return effectiveMinWidth();
    }

    protected float effectiveMinWidth() {
        return minWidth;
    }

    @Override
    public boolean shouldRenderHudPanel() {
        if (!host.isEnabled()) {
            return false;
        }
        if (host.registeredHudPanels().size() <= 1) {
            return true;
        }
        return host.isHudPanelUserVisible(id);
    }

    @Override
    public @Nullable Runnable prepareAndDraw(GuiRenderer glRenderer, float deltaSeconds, boolean editorMode) {
        HudContent content = produceHudContent(deltaSeconds, editorMode);
        if (content == null) {
            return null;
        }
        return HudContentRenderer.prepare(glRenderer, content, host, this, editorMode);
    }

    /**
     * Produces HUD content for this panel ({@link HudHostModule} registers one or more panels; each produces its own content).
     *
     * @param deltaSeconds frame delta in seconds
     * @param editorMode   true inside the HUD layout editor preview
     * @return drawable content, or {@code null} when nothing should draw
     */
    protected abstract @Nullable HudContent produceHudContent(float deltaSeconds, boolean editorMode);

    /**
     * Optional alignment override for text-line HUD entries; {@code null} keeps centered text within the panel band.
     *
     * @return override alignment, or {@code null} for default
     */
    protected HudAnchorContentAlignment.@Nullable Horizontal hudTextLineHorizontalAlignmentOverride() {
        return null;
    }

    public final HudAnchorContentAlignment.Horizontal hudTextLineHorizontalAlignment() {
        HudAnchorContentAlignment.Horizontal override = hudTextLineHorizontalAlignmentOverride();
        if (override != null) {
            return override;
        }
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    public HudAnchorContentAlignment.Horizontal hudContentHorizontalAlignment() {
        HudAnchorContentAlignment.Horizontal anchorAlignment = HudAnchorContentAlignment.horizontal(hudState.getHudAnchor());
        if (anchorAlignment != HudAnchorContentAlignment.Horizontal.CENTER) {
            return anchorAlignment;
        }
        if (hudState.getHudAnchor() == HUDWidgetAnchor.BOTTOM) {
            return hudState.getAnchorOffsetX() < 0f ? HudAnchorContentAlignment.Horizontal.RIGHT : HudAnchorContentAlignment.Horizontal.LEFT;
        }
        return HudAnchorContentAlignment.Horizontal.CENTER;
    }

    public HudAnchorContentAlignment.Vertical hudContentVerticalAlignment() {
        return HudAnchorContentAlignment.vertical(hudState.getHudAnchor());
    }

    public void resetPanelLayoutDefaults() {
        HudDefaults defaults = host.getHudDefaults();
        hudState.setScale(1.0f);
        hudState.setHudAnchor(defaults.defaultAnchor());
        hudState.setAnchorOffsetX(defaults.defaultXOffset());
        hudState.setAnchorOffsetY(defaults.defaultYOffset());
        hudState.setPositionX(defaults.defaultXOffset());
        hudState.setPositionY(defaults.defaultYOffset());
        hudState.setLastLayoutWidth(-1f);
        hudState.setLastLayoutHeight(-1f);
        hudState.setCommittedLayoutHeight(-1f);
        hudState.setCommittedLayoutWidth(-1f);
    }

    public float getScaledWidth() {
        return layoutWidthForAnchoring() * hudState.getScale();
    }

    public float getScaledHeight() {
        return layoutHeightForAnchoring() * hudState.getScale();
    }

    public boolean containsPoint(float pointerX, float pointerY) {
        return pointerX >= hudState.getPositionX()
                && pointerX <= hudState.getPositionX() + getScaledWidth()
                && pointerY >= hudState.getPositionY()
                && pointerY <= hudState.getPositionY() + getScaledHeight();
    }

    public void applyHudAnchorToPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float maxOffsetX = Math.max(0f, canvasWidth - widgetWidth);
        float maxOffsetY = Math.max(0f, canvasHeight - widgetHeight);
        float offsetX = hudState.getAnchorOffsetX();
        float offsetY = hudState.getAnchorOffsetY();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        HudAnchorContentAlignment.Horizontal hAlign = hudContentHorizontalAlignment();

        float resolvedX = switch (hudState.getHudAnchor()) {
            case TOP_LEFT, LEFT -> offsetX;
            case TOP_RIGHT, RIGHT -> canvasWidth - widgetWidth - offsetX;
            case BOTTOM_LEFT -> offsetX;
            case BOTTOM_RIGHT -> canvasWidth - widgetWidth - offsetX;
            case TOP, CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
            case BOTTOM -> switch (hAlign) {
                case LEFT -> halfCanvasWidth + offsetX;
                case RIGHT -> halfCanvasWidth - widgetWidth + offsetX;
                case CENTER -> halfCanvasWidth - halfWidgetWidth + offsetX;
            };
        };

        float resolvedY = switch (hudState.getHudAnchor()) {
            case TOP_LEFT, TOP_RIGHT, TOP -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT, BOTTOM -> canvasHeight - widgetHeight - offsetY;
            case LEFT, RIGHT -> halfCanvasHeight - halfWidgetHeight + offsetY;
            case CENTER -> halfCanvasHeight - halfWidgetHeight + offsetY;
        };

        hudState.setPositionX(Mth.clamp(resolvedX, 0f, maxOffsetX));
        hudState.setPositionY(Mth.clamp(resolvedY, 0f, maxOffsetY));
    }

    public void captureNearestHudAnchorFromPosition(float canvasWidth, float canvasHeight) {
        hudState.setHudAnchor(nearestAnchor(canvasWidth, canvasHeight));
        syncHudAnchorOffsetsFromCurrentPosition(canvasWidth, canvasHeight);
    }

    public void syncHudAnchorOffsetsFromCurrentPosition(float canvasWidth, float canvasHeight) {
        float widgetWidth = getScaledWidth();
        float widgetHeight = getScaledHeight();
        float halfCanvasWidth = canvasWidth * 0.5f;
        float halfCanvasHeight = canvasHeight * 0.5f;
        float halfWidgetWidth = widgetWidth * 0.5f;
        float halfWidgetHeight = widgetHeight * 0.5f;
        HudAnchorContentAlignment.Horizontal hAlign = hudContentHorizontalAlignment();
        float posX = hudState.getPositionX();
        float posY = hudState.getPositionY();

        switch (hudState.getHudAnchor()) {
            case TOP_LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(posY);
            }
            case TOP_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(posY);
            }
            case BOTTOM_LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case BOTTOM_RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case TOP -> {
                hudState.setAnchorOffsetX(posX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(posY);
            }
            case BOTTOM -> {
                float ox = switch (hAlign) {
                    case LEFT -> posX - halfCanvasWidth;
                    case RIGHT -> posX + widgetWidth - halfCanvasWidth;
                    case CENTER -> posX + halfWidgetWidth - halfCanvasWidth;
                };
                hudState.setAnchorOffsetX(ox);
                hudState.setAnchorOffsetY(canvasHeight - posY - widgetHeight);
            }
            case LEFT -> {
                hudState.setAnchorOffsetX(posX);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
            case RIGHT -> {
                hudState.setAnchorOffsetX(canvasWidth - posX - widgetWidth);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
            case CENTER -> {
                hudState.setAnchorOffsetX(posX + halfWidgetWidth - halfCanvasWidth);
                hudState.setAnchorOffsetY(posY + halfWidgetHeight - halfCanvasHeight);
            }
        }
    }

    public final void recordHudContentSkipped() {
        if (hudState.getLastLayoutWidth() < 0f || hudState.getLastLayoutHeight() < 0f) {
            float fallback = Math.max(1f, minWidth);
            hudState.setLastLayoutWidth(fallback);
            hudState.setLastLayoutHeight(fallback);
            hudState.setCommittedLayoutWidth(fallback);
            hudState.setCommittedLayoutHeight(fallback);
        }
    }

    JsonObject serializeHudState(Gson gson) {
        JsonObject hudJson = new JsonObject();
        hudJson.addProperty("scale", hudState.getScale());
        hudJson.addProperty("anchor", hudState.getHudAnchor().name());
        hudJson.addProperty("anchor_offset_x", hudState.getAnchorOffsetX());
        hudJson.addProperty("anchor_offset_y", hudState.getAnchorOffsetY());
        hudJson.addProperty("last_layout_width", hudState.getLastLayoutWidth());
        hudJson.addProperty("last_layout_height", hudState.getLastLayoutHeight());
        return hudJson;
    }

    void deserializeHudState(JsonObject hudJson, Gson gson) {
        if (hudJson.has("scale")) {
            hudState.setScale(hudJson.get("scale").getAsFloat());
        }
        if (hudJson.has("anchor")) {
            try {
                hudState.setHudAnchor(HUDWidgetAnchor.valueOf(hudJson.get("anchor").getAsString()));
            } catch (IllegalArgumentException ignored) {
                hudState.setHudAnchor(HUDWidgetAnchor.TOP_LEFT);
            }
        }
        if (hudJson.has("anchor_offset_x")) {
            hudState.setAnchorOffsetX(hudJson.get("anchor_offset_x").getAsFloat());
        }
        if (hudJson.has("anchor_offset_y")) {
            hudState.setAnchorOffsetY(hudJson.get("anchor_offset_y").getAsFloat());
        }
        if (hudJson.has("last_layout_width")) {
            float width = hudJson.get("last_layout_width").getAsFloat();
            hudState.setLastLayoutWidth(width);
            if (width > 0f && Float.isFinite(width)) {
                hudState.setCommittedLayoutWidth(width);
            }
        }
        if (hudJson.has("last_layout_height")) {
            float height = hudJson.get("last_layout_height").getAsFloat();
            hudState.setLastLayoutHeight(height);
            if (height > 0f && Float.isFinite(height)) {
                hudState.setCommittedLayoutHeight(height);
            }
        }
    }

    private float layoutWidthForAnchoring() {
        if (hudState.getCommittedLayoutWidth() >= 0f) {
            return hudState.getCommittedLayoutWidth();
        }
        if (hudState.getLastLayoutWidth() >= 0f) {
            return hudState.getLastLayoutWidth();
        }
        return minWidth;
    }

    private float layoutHeightForAnchoring() {
        if (hudState.getCommittedLayoutHeight() >= 0f) {
            return hudState.getCommittedLayoutHeight();
        }
        if (hudState.getLastLayoutHeight() >= 0f) {
            return hudState.getLastLayoutHeight();
        }
        return minWidth;
    }

    private HUDWidgetAnchor nearestAnchor(float canvasWidth, float canvasHeight) {
        float centerX = hudState.getPositionX() + getScaledWidth() * 0.5f;
        float centerY = hudState.getPositionY() + getScaledHeight() * 0.5f;
        HUDWidgetAnchor chosen = HUDWidgetAnchor.TOP_LEFT;
        float bestDist = Float.MAX_VALUE;
        for (HUDWidgetAnchor candidate : HUDWidgetAnchor.values()) {
            float dx = centerX - candidate.referenceX(canvasWidth);
            float dy = centerY - candidate.referenceY(canvasHeight);
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                chosen = candidate;
            }
        }
        return chosen;
    }
}
