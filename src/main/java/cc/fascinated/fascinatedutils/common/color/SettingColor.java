package cc.fascinated.fascinatedutils.common.color;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SettingColor {

    private int red;
    private int green;
    private int blue;
    private int alpha;
    private boolean rainbow;

    public SettingColor() {
        this(255, 255, 255, 255, false);
    }

    public SettingColor(int red, int green, int blue) {
        this(red, green, blue, 255, false);
    }

    public SettingColor(int red, int green, int blue, int alpha) {
        this(red, green, blue, alpha, false);
    }

    public SettingColor(int red, int green, int blue, int alpha, boolean rainbow) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        this.rainbow = rainbow;
        validate();
    }

    public SettingColor(SettingColor other) {
        this.red = other.red;
        this.green = other.green;
        this.blue = other.blue;
        this.alpha = other.alpha;
        this.rainbow = other.rainbow;
    }

    /**
     * Creates a SettingColor from a packed ARGB integer.
     *
     * @param argb packed ARGB color
     * @return a new SettingColor
     */
    public static SettingColor fromArgb(int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >> 16) & 0xFF;
        int green = (argb >> 8) & 0xFF;
        int blue = argb & 0xFF;
        return new SettingColor(red, green, blue, alpha);
    }

    /**
     * Creates a SettingColor from HSV values.
     *
     * @param hue        hue in degrees (0-360)
     * @param saturation saturation (0.0-1.0)
     * @param value      brightness value (0.0-1.0)
     * @return a new SettingColor
     */
    public static SettingColor fromHsv(float hue, float saturation, float value) {
        float chroma = value * saturation;
        float hueSegment = hue / 60f;
        float intermediate = chroma * (1f - Math.abs(hueSegment % 2f - 1f));
        float red = 0f;
        float green = 0f;
        float blue = 0f;

        if (hueSegment <= 1f) {
            red = chroma;
            green = intermediate;
        }
        else if (hueSegment <= 2f) {
            red = intermediate;
            green = chroma;
        }
        else if (hueSegment <= 3f) {
            green = chroma;
            blue = intermediate;
        }
        else if (hueSegment <= 4f) {
            green = intermediate;
            blue = chroma;
        }
        else if (hueSegment <= 5f) {
            red = intermediate;
            blue = chroma;
        }
        else {
            red = chroma;
            blue = intermediate;
        }

        float lightnessAdjust = value - chroma;
        return new SettingColor(Math.round((red + lightnessAdjust) * 255f), Math.round((green + lightnessAdjust) * 255f), Math.round((blue + lightnessAdjust) * 255f), 255);
    }

    private static int clampChannel(int channel) {
        return Math.max(0, Math.min(255, channel));
    }

    /**
     * Returns the resolved packed ARGB color, applying rainbow if enabled.
     *
     * @return packed ARGB integer
     */
    public int getResolvedArgb() {
        if (rainbow) {
            SettingColor rainbowColor = RainbowColors.currentColor();
            return ((alpha & 0xFF) << 24) | ((rainbowColor.red & 0xFF) << 16) | ((rainbowColor.green & 0xFF) << 8) | (rainbowColor.blue & 0xFF);
        }
        return getPackedArgb();
    }

    /**
     * Returns the packed ARGB color without rainbow resolution.
     *
     * @return packed ARGB integer
     */
    public int getPackedArgb() {
        return ((alpha & 0xFF) << 24) | ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);
    }

    /**
     * Copies values from another SettingColor.
     *
     * @param other the source color
     * @return this instance for chaining
     */
    public SettingColor set(SettingColor other) {
        this.red = other.red;
        this.green = other.green;
        this.blue = other.blue;
        this.alpha = other.alpha;
        this.rainbow = other.rainbow;
        return this;
    }

    /**
     * Returns a deep copy of this color.
     *
     * @return a new SettingColor with the same values
     */
    public SettingColor copy() {
        return new SettingColor(this);
    }

    /**
     * Extracts hue, saturation, and value from the RGB components.
     *
     * @return float array [hue (0-360), saturation (0-1), value (0-1)]
     */
    public float[] toHsv() {
        float normalizedRed = red / 255f;
        float normalizedGreen = green / 255f;
        float normalizedBlue = blue / 255f;
        float max = Math.max(normalizedRed, Math.max(normalizedGreen, normalizedBlue));
        float min = Math.min(normalizedRed, Math.min(normalizedGreen, normalizedBlue));
        float delta = max - min;

        float hue = 0f;
        if (delta > 0f) {
            if (max == normalizedRed) {
                hue = 60f * (((normalizedGreen - normalizedBlue) / delta) % 6f);
            }
            else if (max == normalizedGreen) {
                hue = 60f * (((normalizedBlue - normalizedRed) / delta) + 2f);
            }
            else {
                hue = 60f * (((normalizedRed - normalizedGreen) / delta) + 4f);
            }
        }
        if (hue < 0f) {
            hue += 360f;
        }
        float saturation = max > 0f ? delta / max : 0f;
        return new float[]{hue, saturation, max};
    }

    /**
     * Serializes this color to a JSON object.
     *
     * @return JSON representation
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("r", red);
        json.addProperty("g", green);
        json.addProperty("b", blue);
        json.addProperty("a", alpha);
        json.addProperty("rainbow", rainbow);
        return json;
    }

    /**
     * Deserializes a color from a JSON object.
     *
     * @param json JSON source
     * @return this instance for chaining
     */
    public SettingColor fromJson(JsonObject json) {
        if (json.has("r")) {
            red = json.get("r").getAsInt();
        }
        if (json.has("g")) {
            green = json.get("g").getAsInt();
        }
        if (json.has("b")) {
            blue = json.get("b").getAsInt();
        }
        if (json.has("a")) {
            alpha = json.get("a").getAsInt();
        }
        if (json.has("rainbow")) {
            rainbow = json.get("rainbow").getAsBoolean();
        }
        validate();
        return this;
    }

    private void validate() {
        red = clampChannel(red);
        green = clampChannel(green);
        blue = clampChannel(blue);
        alpha = clampChannel(alpha);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        SettingColor that = (SettingColor) other;
        return red == that.red && green == that.green && blue == that.blue && alpha == that.alpha && rainbow == that.rainbow;
    }

    @Override
    public int hashCode() {
        int result = red;
        result = 31 * result + green;
        result = 31 * result + blue;
        result = 31 * result + alpha;
        result = 31 * result + (rainbow ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SettingColor{r=" + red + ", g=" + green + ", b=" + blue + ", a=" + alpha + ", rainbow=" + rainbow + "}";
    }
}
