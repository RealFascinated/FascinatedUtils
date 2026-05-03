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
    private final Supplier<String> nameProvider;
    private final Supplier<@Nullable String> tooltipProvider;
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
        this.nameProvider = builder.nameProvider != null
                ? builder.nameProvider
                : this::resolveTranslatedDisplayName;
        this.tooltipProvider = builder.tooltipProvider != null
                ? builder.tooltipProvider
                : this::resolveTranslatedTooltip;
    }

    private String resolveTranslatedDisplayName() {
        String keyBase = translationKeyPrefix + "." + settingKey;
        return I18n.get(keyBase + ".display_name");
    }

    private @Nullable String resolveTranslatedTooltip() {
        String key = translationKeyPrefix + "." + settingKey + ".description";
        if (!I18n.exists(key)) {
            return null;
        }
        String translated = I18n.get(key);
        return translated.isBlank() ? null : translated;
    }

    public String getName() {
        return nameProvider.get();
    }

    public @Nullable String getTooltip() {
        return tooltipProvider.get();
    }

    public void setTranslationKeyPrefix(String translationKeyPrefix) {
        this.translationKeyPrefix = Objects.requireNonNull(translationKeyPrefix, "translation key prefix is required");
    }

    public boolean isLocked() {
        return locked != null && Boolean.TRUE.equals(locked.get());
    }

    public @Nullable String getLockedReasonText() {
        if (lockedReason == null) {
            return null;
        }
        String reason = lockedReason.get();
        return reason == null || reason.isBlank() ? null : reason;
    }

    public boolean isAtDefault() {
        return Objects.equals(value, defaultValue);
    }

    public void resetToDefault() {
        this.value = this.defaultValue;
    }

    public void addSubSetting(Setting<?> sub) {
        sub.subSetting = true;
        subSettings.add(sub);
    }

    public List<Setting<?>> getSubSettings() {
        return Collections.unmodifiableList(subSettings);
    }

    public boolean hasSubSettings() {
        return !subSettings.isEmpty();
    }

    public @Nullable JsonElement serializeValue() {
        return null;
    }

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
        private @Nullable Supplier<String> nameProvider;
        private @Nullable Supplier<@Nullable String> tooltipProvider;

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

        public B displayName(Supplier<String> displayNameProvider) {
            this.nameProvider = displayNameProvider;
            return self();
        }

        public B displayName(String displayName) {
            this.nameProvider = () -> displayName;
            return self();
        }

        public B tooltip(Supplier<@Nullable String> tooltipProvider) {
            this.tooltipProvider = tooltipProvider;
            return self();
        }

        public B tooltip(String tooltip) {
            this.tooltipProvider = () -> tooltip;
            return self();
        }

        public abstract Setting<T> build();

        protected abstract B self();
    }
}