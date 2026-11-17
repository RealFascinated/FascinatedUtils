package cc.fascinated.fascinatedutils.mixin.chat;

import cc.fascinated.fascinatedutils.systems.chat.ChatSearch;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.chat.ChatModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    private static final int SEARCH_BOX_BOTTOM_PADDING = 6;
    private static final int ESCAPE_KEY = 256;
    private static final int DOWN_KEY = 264;
    private static final int UP_KEY = 265;

    @Shadow
    protected EditBox input;

    @Shadow
    public abstract void moveInHistory(int dir);

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
        EditBox searchBox = new EditBox(font, input.getX(), input.getY() - SEARCH_BOX_HEIGHT - SEARCH_BOX_BOTTOM_PADDING, SEARCH_BOX_WIDTH, SEARCH_BOX_HEIGHT, Component.translatable("alumite.module.chat.search_hint"));
        searchBox.setHint(Component.translatable("alumite.module.chat.search_hint").withStyle(ChatFormatting.DARK_GRAY));
        searchBox.setMaxLength(64);
        searchBox.setValue(ChatSearch.INSTANCE.getQuery());
        searchBox.setResponder(query -> ChatSearch.INSTANCE.apply(minecraft.gui.getChat(), query));
        addRenderableWidget(searchBox);
        alumite$searchBox = searchBox;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void alumite$updateSearchVisibility(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (alumite$searchBox == null) {
            return;
        }
        alumite$searchBox.visible = input.getValue().isEmpty();
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
        if (alumite$searchBox == null) {
            return;
        }
        boolean searchFocused = alumite$searchBox.visible && alumite$searchBox.isFocused();
        if (!searchFocused && (event.key() == UP_KEY || event.key() == DOWN_KEY)) {
            // Screen arrow navigation would otherwise move focus to the search box instead of browsing history
            if (!input.isFocused()) {
                setFocused(input);
            }
            moveInHistory(event.key() == UP_KEY ? -1 : 1);
            cir.setReturnValue(true);
            return;
        }
        if (!searchFocused || event.key() == ESCAPE_KEY) {
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
