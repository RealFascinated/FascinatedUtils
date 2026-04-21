package cc.fascinated.fascinatedutils.common.setting;

import cc.fascinated.fascinatedutils.systems.config.GsonSerializable;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.resources.language.I18n;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

@Getter
@Setter
public class Setting<T> implements GsonSerializable<Setting<T>> {
    private final String settingKey;
    private final T defaultValue;
    private final @Nullable String categoryDisplayKey;
    private final @Nullable Supplier<Boolean> locked;
    private final @Nullable Supplier<String> lockedReason;
    private final @Nullable String translationKeyPath;
    private String translationKeyPrefix = "fascinatedutils.setting";
    private T value;
    private final List<Setting<?>> subSettings = new ArrayList<>();
    private boolean subSetting;

    protected Setting(Builder<T, ?> builder) {
        this.settingKey = Objects.requireNonNull(builder.id, "setting id is required");
        this.defaultValue = Objects.requireNonNull(builder.defaultValue, "default value is required");
        this.value = builder.value != null ? builder.value : this.defaultValue;
        this.categoryDisplayKey = builder.categoryDisplayKey;
        this.locked = builder.locked;
        this.lockedReason = builder.lockedReason;
        this.translationKeyPath = builder.translationKeyPath;
    }

    /**
     * Resolves {@link #settingKey} into a Minecraft translation key for UI labels.
     */
    public String getTranslatedDisplayName() {
        String keyBase = translationKeyPath != null ? translationKeyPath : translationKeyPrefix + "." + settingKey;
        String displayNameTranslationKey = keyBase + ".display_name";
        return I18n.get(displayNameTranslationKey);
    }

    public void setTranslationKeyPrefix(String translationKeyPrefix) {
        this.translationKeyPrefix = Objects.requireNonNull(translationKeyPrefix, "translation key prefix is required");
    }

    public boolean isLocked() {
        Supplier<Boolean> lockState = locked;
        return lockState != null && Boolean.TRUE.equals(lockState.get());
    }

    public @Nullable String getLockedReasonText() {
        Supplier<String> reasonSupplier = lockedReason;
        if (reasonSupplier == null) {
            return null;
        }
        String reason = reasonSupplier.get();
        return reason == null || reason.isBlank() ? null : reason;
    }

    public @Nullable String getTooltipDescriptionText() {
        String keyBase = translationKeyPath != null ? translationKeyPath : translationKeyPrefix + "." + settingKey;
        String descriptionTranslationKey = keyBase + ".description";
        if (!I18n.exists(descriptionTranslationKey)) {
            return null;
        }
        String translated = I18n.get(descriptionTranslationKey);
        return translated == null || translated.isBlank() ? null : translated;
    }

    public boolean isAtDefault() {
        return Objects.equals(value, defaultValue);
    }

    /**
     * Restores {@link #value} from {@link #defaultValue}. Specialized settings may override to keep side effects in sync.
     */
    public void resetToDefault() {
        this.value = this.defaultValue;
    }

    /**
     * Adds a child setting that is logically subordinate to this one (e.g., a radius slider under a rounded-corners
     * toggle). The child is marked as a sub-setting so UI builders can skip it from the flat setting list and render
     * it only inside the parent's expanded sub-panel.
     *
     * @param sub the child setting
     */
    public void addSubSetting(Setting<?> sub) {
        sub.subSetting = true;
        subSettings.add(sub);
    }

    /**
     * Returns an unmodifiable view of child settings registered via {@link #addSubSetting}.
     *
     * @return sub-settings, may be empty
     */
    public List<Setting<?>> getSubSettings() {
        return Collections.unmodifiableList(subSettings);
    }

    /**
     * Whether this setting has any child settings registered via {@link #addSubSetting}.
     *
     * @return true when sub-settings exist
     */
    public boolean hasSubSettings() {
        return !subSettings.isEmpty();
    }

    /**
     * Serializes the current value to a JSON element for persistence.
     * Subclasses should override to provide type-specific serialization.
     *
     * @return a JSON element representing the current value, or {@code null} if not serializable
     */
    public @Nullable JsonElement serializeValue() {
        return null;
    }

    /**
     * Deserializes a value from a JSON element and applies it to this setting.
     * Subclasses should override to provide type-specific deserialization.
     *
     * @param json the JSON element to deserialize from
     */
    public void deserializeValue(@Nullable JsonElement json) {
    }

    @Override
    public JsonElement serialize(Gson gson) {
        JsonElement serialized = serializeValue();
        return serialized != null ? serialized : JsonNull.INSTANCE;
    }

    @Override
    public Setting<T> deserialize(JsonElement data, Gson gson) {
        deserializeValue(data);
        return this;
    }

    public static abstract class Builder<T, B extends Builder<T, B>> {
        private String id;
        private T defaultValue;
        private T value;
        private @Nullable String categoryDisplayKey;
        private @Nullable Supplier<Boolean> locked;
        private @Nullable Supplier<String> lockedReason;
        private @Nullable String translationKeyPath;

        public B id(String id) {
            this.id = id;
            return self();
        }

        public B defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return self();
        }

        public B value(T value) {
            this.value = value;
            return self();
        }

        public B categoryDisplayKey(@Nullable String categoryDisplayKey) {
            this.categoryDisplayKey = categoryDisplayKey;
            return self();
        }

        public B locked(@Nullable Supplier<Boolean> locked) {
            this.locked = locked;
            return self();
        }

        public B lockedReason(@Nullable Supplier<String> lockedReason) {
            this.lockedReason = lockedReason;
            return self();
        }

        public B translationKeyPath(@Nullable String translationKeyPath) {
            this.translationKeyPath = translationKeyPath;
            return self();
        }

        public abstract Setting<T> build();

        protected abstract B self();
    }
}
