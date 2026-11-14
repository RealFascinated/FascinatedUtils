package cc.fascinated.fascinatedutils.mixin.chat;

import cc.fascinated.fascinatedutils.systems.chat.ChatSearch;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ChatModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    private static final int SEARCH_BOX_WIDTH = 150;
    private static final int SEARCH_BOX_HEIGHT = 14;
    private static final int ESCAPE_KEY = 256;

    @Shadow
    protected EditBox input;

    @Unique
    private EditBox alumite$searchBox;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void alumite$addSearchBox(CallbackInfo ci) {
        alumite$searchBox = null;
        Optional<ChatModule> moduleOptional = ModuleRegistry.INSTANCE.getModule(ChatModule.class);
        if (moduleOptional.isEmpty() || !moduleOptional.get().isEnabled() || !moduleOptional.get().getChatSearch().isEnabled()) {
            return;
        }
        EditBox searchBox = new EditBox(font, width - SEARCH_BOX_WIDTH - 4, 4, SEARCH_BOX_WIDTH, SEARCH_BOX_HEIGHT, Component.translatable("alumite.module.chat.search_hint"));
        searchBox.setHint(Component.translatable("alumite.module.chat.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(64);
        searchBox.setValue(ChatSearch.INSTANCE.getQuery());
        searchBox.setResponder(query -> ChatSearch.INSTANCE.apply(minecraft.gui.getChat(), query));
        addRenderableWidget(searchBox);
        alumite$searchBox = searchBox;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void alumite$searchMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (alumite$searchBox == null) {
            return;
        }
        if (alumite$searchBox.isMouseOver(event.x(), event.y())) {
            input.setCanLoseFocus(true);
            setFocused(alumite$searchBox);
            input.setCanLoseFocus(false);
            alumite$searchBox.mouseClicked(event, doubleClick);
            cir.setReturnValue(true);
        } else if (alumite$searchBox.isFocused()) {
            setFocused(input);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void alumite$searchKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (alumite$searchBox == null || !alumite$searchBox.isFocused() || event.key() == ESCAPE_KEY) {
            return;
        }
        alumite$searchBox.keyPressed(event);
        cir.setReturnValue(true);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void alumite$clearSearch(CallbackInfo ci) {
        if (ChatSearch.INSTANCE.isActive()) {
            ChatSearch.INSTANCE.apply(minecraft.gui.getChat(), "");
        }
    }
}
