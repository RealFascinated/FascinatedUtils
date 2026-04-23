package cc.fascinated.fascinatedutils.systems.modules.impl;

import cc.fascinated.fascinatedutils.client.keybind.KeybindsWrapper;
import cc.fascinated.fascinatedutils.common.setting.impl.KeybindSetting;
import cc.fascinated.fascinatedutils.event.impl.ClientTickEvent;
import cc.fascinated.fascinatedutils.systems.modules.Module;
import cc.fascinated.fascinatedutils.systems.modules.ModuleCategory;
import cc.fascinated.fascinatedutils.systems.modules.ModuleDefaults;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

@Getter
public class FreelookModule extends Module {
    private static final float MAX_PITCH = 90f;

    private final KeyMapping freelookKeyBinding = KeybindsWrapper.registerKeybind("key.fascinatedutils.freelook", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KeybindsWrapper.CATEGORY);
    private final KeybindSetting freelookKeySetting = KeybindSetting.builder().id("freelook_key").defaultValue("").keyBindingSupplier(() -> freelookKeyBinding).categoryDisplayKey("Controls").build();

    private float freelookYaw = 0f;
    private float freelookPitch = 0f;
    private boolean wasActive = false;
    private CameraType savedCameraType = null;

    public FreelookModule() {
        super("Freelook", ModuleCategory.GENERAL, ModuleDefaults.builder().defaultState(true).build());
        addSetting(freelookKeySetting);
    }

    public boolean isFreelookActive() {
        return isEnabled() && freelookKeyBinding.isDown();
    }

    public void addMouseDelta(double dx, double dy) {
        freelookYaw += (float) dx * 0.15f;
        freelookPitch = Mth.clamp(freelookPitch + (float) dy * 0.15f, -MAX_PITCH, MAX_PITCH);
    }

    @EventHandler
    private void onClientTick(ClientTickEvent event) {
        boolean active = isFreelookActive();
        if (active == wasActive) {
            return;
        }
        wasActive = active;
        Minecraft minecraft = Minecraft.getInstance();
        if (active) {
            savedCameraType = minecraft.options.getCameraType();
            minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
        else {
            freelookYaw = 0f;
            freelookPitch = 0f;
            if (savedCameraType != null) {
                minecraft.options.setCameraType(savedCameraType);
                savedCameraType = null;
            }
        }
    }
}
