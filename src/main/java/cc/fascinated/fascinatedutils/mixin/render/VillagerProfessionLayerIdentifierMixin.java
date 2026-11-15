package cc.fascinated.fascinatedutils.mixin.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VillagerProfessionLayer.class)
public class VillagerProfessionLayerIdentifierMixin {

    // Every villager asks for the same profession texture up to three times per frame, and each miss concatenates the
    // path, validates it and copies the string into a fresh identifier. The type strings are a couple of constants and
    // the input identifiers come from the profession holder, so these maps stay bounded and only ever hold identifiers.
    @Unique
    private final Map<String, Map<Identifier, Identifier>> alumite$identifiers = new HashMap<>();

    @Inject(method = "getIdentifier(Ljava/lang/String;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/Identifier;", at = @At("HEAD"), cancellable = true)
    private void alumite$reuseIdentifier(String type, Identifier key, CallbackInfoReturnable<Identifier> info) {
        Map<Identifier, Identifier> cachedByIdentifier = this.alumite$identifiers.get(type);
        if (cachedByIdentifier == null) {
            cachedByIdentifier = new HashMap<>();
            this.alumite$identifiers.put(type, cachedByIdentifier);
        }

        Identifier cached = cachedByIdentifier.get(key);
        if (cached != null) {
            info.setReturnValue(cached);
        }
    }

    @Inject(method = "getIdentifier(Ljava/lang/String;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/Identifier;", at = @At("RETURN"))
    private void alumite$rememberIdentifier(String type, Identifier key, CallbackInfoReturnable<Identifier> info) {
        Map<Identifier, Identifier> cachedByIdentifier = this.alumite$identifiers.get(type);
        if (cachedByIdentifier == null) {
            cachedByIdentifier = new HashMap<>();
            this.alumite$identifiers.put(type, cachedByIdentifier);
        }

        cachedByIdentifier.put(key, info.getReturnValue());
    }
}
