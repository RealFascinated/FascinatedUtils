package cc.fascinated.fascinatedutils.common.setting.impl;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.mixin.KeyBindingAccessorMixin;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.Supplier;

public class KeybindSetting extends Setting<String> {
    private final KeyMapping keyMapping;

    public KeybindSetting(String id, Supplier<KeyMapping> keyBindingSupplier) {
        this(builder().id(id).defaultValue("").keyBindingSupplier(keyBindingSupplier));
    }

    private KeybindSetting(Builder builder) {
        super(builder);
        keyMapping = Objects.requireNonNull(builder.keyBindingSupplier.get(), "key binding supplier is required");
    }

    public static Builder builder() {
        return new Builder();
    }

    public String currentBindingLabel() {
        if (this.keyMapping == null) {
            return "...";
        }
        return this.keyMapping .getTranslatedKeyMessage().getString();
    }

    @Override
    public boolean isAtDefault() {
        if (this.keyMapping == null) {
            return true;
        }
        InputConstants.Key currentKey = ((KeyBindingAccessorMixin) this.keyMapping).getKey();
        return currentKey.equals(this.keyMapping.getDefaultKey());
    }

    public void applyBinding(InputConstants.Key nextKey) {
        if (this.keyMapping == null || nextKey == null) {
            return;
        }
        this.keyMapping.setKey(nextKey);
        KeyMapping.resetMapping();
        Minecraft minecraftClient = Minecraft.getInstance();
        minecraftClient.options.save();
    }

    /**
     * Restores the backing {@link KeyMapping} to its registration default and clears the shadow string value.
     */
    @Override
    public void resetToDefault() {
        if (this.keyMapping != null) {
            InputConstants.Key defaultKey = this.keyMapping.getDefaultKey();
            this.keyMapping.setKey(defaultKey);
            KeyMapping.resetMapping();
            Minecraft minecraftClient = Minecraft.getInstance();
            //noinspection ConstantValue
            if (minecraftClient != null && minecraftClient.options != null) {
                minecraftClient.options.save();
            }
        }
        super.resetToDefault();
    }

    public static class Builder extends Setting.Builder<String, Builder> {
        private Supplier<KeyMapping> keyBindingSupplier;

        public Builder keyBindingSupplier(Supplier<KeyMapping> keyBindingSupplier) {
            this.keyBindingSupplier = keyBindingSupplier;
            return this;
        }

        @Override
        public KeybindSetting build() {
            return new KeybindSetting(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
