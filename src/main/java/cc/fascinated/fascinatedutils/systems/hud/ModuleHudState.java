package cc.fascinated.fascinatedutils.systems.hud;

import cc.fascinated.fascinatedutils.systems.hud.anchor.HUDWidgetAnchor;
import cc.fascinated.fascinatedutils.systems.hud.panel.HudPanel;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleHudState {
    private float positionX = 5f;
    private float positionY = 5f;
    private float anchorOffsetX = 5f;
    private float anchorOffsetY = 5f;
    private float scale = 1.0f;
    private HUDWidgetAnchor hudAnchor = HUDWidgetAnchor.TOP_LEFT;
    /**
     * For the {@code BOTTOM} anchor only: whether the panel hangs left of the centre line (offset measured
     * from its right edge) instead of right of it (offset measured from its left edge).
     */
    private boolean bottomAnchorRightAligned = false;

    private float lastLayoutWidth = -1f;
    private float lastLayoutHeight = -1f;
    private float committedLayoutWidth = -1f;
    private float committedLayoutHeight = -1f;

    /**
     * Corners (see {@code RectCornerRoundMask}) drawn square because another HUD panel touches them.
     */
    private int squaredCornerMask;

    /**
     * Writes this layout state to JSON for the profile file.
     *
     * @return the serialised state
     */
    public JsonObject toJson() {
        JsonObject hudJson = new JsonObject();
        hudJson.addProperty("scale", scale);
        hudJson.addProperty("anchor", hudAnchor.name());
        hudJson.addProperty("anchor_offset_x", getAnchorOffsetX());
        hudJson.addProperty("anchor_offset_y", getAnchorOffsetY());
        hudJson.addProperty("bottom_anchor_right_aligned", isBottomAnchorRightAligned());
        hudJson.addProperty("last_layout_width", getLastLayoutWidth());
        hudJson.addProperty("last_layout_height", getLastLayoutHeight());
        return hudJson;
    }

    /**
     * Restores layout state from a profile file, ignoring malformed values.
     *
     * @param hudJson the serialised state
     */
    public void readJson(JsonObject hudJson) {
        if (hudJson.has("scale")) {
            float storedScale = hudJson.get("scale").getAsFloat();
            if (Float.isFinite(storedScale)) {
                setScale(Math.clamp(storedScale, HudPanel.MIN_SCALE, HudPanel.MAX_SCALE));
            }
        }
        if (hudJson.has("anchor")) {
            try {
                setHudAnchor(HUDWidgetAnchor.valueOf(hudJson.get("anchor").getAsString()));
            } catch (IllegalArgumentException ignored) {
                setHudAnchor(HUDWidgetAnchor.TOP_LEFT);
            }
        }
        if (hudJson.has("anchor_offset_x")) {
            float storedOffsetX = hudJson.get("anchor_offset_x").getAsFloat();
            if (Float.isFinite(storedOffsetX)) {
                setAnchorOffsetX(storedOffsetX);
            }
        }
        if (hudJson.has("anchor_offset_y")) {
            float storedOffsetY = hudJson.get("anchor_offset_y").getAsFloat();
            if (Float.isFinite(storedOffsetY)) {
                setAnchorOffsetY(storedOffsetY);
            }
        }
        if (hudJson.has("bottom_anchor_right_aligned")) {
            setBottomAnchorRightAligned(hudJson.get("bottom_anchor_right_aligned").getAsBoolean());
        } else {
            // Layouts saved before the flag existed derived the side from the sign of the offset.
            setBottomAnchorRightAligned(getAnchorOffsetX() < 0f);
        }
        if (hudJson.has("last_layout_width")) {
            float width = hudJson.get("last_layout_width").getAsFloat();
            setLastLayoutWidth(width);
            if (width > 0f && Float.isFinite(width)) {
                setCommittedLayoutWidth(width);
            }
        }
        if (hudJson.has("last_layout_height")) {
            float height = hudJson.get("last_layout_height").getAsFloat();
            setLastLayoutHeight(height);
            if (height > 0f && Float.isFinite(height)) {
                setCommittedLayoutHeight(height);
            }
        }
    }
}
