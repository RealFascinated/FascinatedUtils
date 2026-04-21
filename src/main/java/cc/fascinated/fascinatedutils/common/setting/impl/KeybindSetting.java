package cc.fascinated.fascinatedutils.common.setting.impl;

import cc.fascinated.fascinatedutils.common.setting.Setting;
import cc.fascinated.fascinatedutils.mixin.KeyBindingAccessorMixin;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.Supplier;

public class KeybindSetting extends Setting<String> {
    private final Supplier<KeyMapping> keyBindingSupplier;

    public KeybindSetting(String id, Supplier<KeyMapping> keyBindingSupplier) {
        this(builder().id(id).defaultValue("").keyBindingSupplier(keyBindingSupplier));
    }

    private KeybindSetting(Builder builder) {
        super(builder);
        this.keyBindingSupplier = Objects.requireNonNull(builder.keyBindingSupplier, "key binding supplier is required");
    }

    public static Builder builder() {
        return new Builder();
    }

    public KeyMapping keyBinding() {
        return keyBindingSupplier.get();
    }

    public String currentBindingLabel() {
        KeyMapping keyBinding = keyBinding();
        if (keyBinding == null) {
            return "...";
        }
        return keyBinding.getTranslatedKeyMessage().getString();
    }

    @Override
    public boolean isAtDefault() {
        KeyMapping keyBinding = keyBinding();
        if (keyBinding == null) {
            return true;
        }
        InputConstants.Key currentKey = ((KeyBindingAccessorMixin) keyBinding).getKey();
        return currentKey.equals(keyBinding.getDefaultKey());
    }

    public void applyBinding(InputConstants.Key nextKey) {
        KeyMapping keyBinding = keyBinding();
        if (keyBinding == null || nextKey == null) {
            return;
        }
        keyBinding.setKey(nextKey);
        KeyMapping.resetMapping();
        Minecraft minecraftClient = Minecraft.getInstance();
        minecraftClient.options.save();
    }

    /**
     * Restores the backing {@link KeyMapping} to its registration default and clears the shadow string value.
     */
    @Override
    public void resetToDefault() {
        KeyMapping keyBinding = keyBinding();
        if (keyBinding != null) {
            InputConstants.Key defaultKey = keyBinding.getDefaultKey();
            keyBinding.setKey(defaultKey);
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
