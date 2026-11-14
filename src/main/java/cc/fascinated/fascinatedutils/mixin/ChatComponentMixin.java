package cc.fascinated.fascinatedutils.mixin;

import cc.fascinated.fascinatedutils.event.FascinatedEventBus;
import cc.fascinated.fascinatedutils.event.impl.chat.ChatMessageEvent;
import cc.fascinated.fascinatedutils.mixininterface.ChatComponentAccess;
import cc.fascinated.fascinatedutils.systems.chat.ChatHistory;
import cc.fascinated.fascinatedutils.systems.chat.ChatSearch;
import cc.fascinated.fascinatedutils.systems.modules.ModuleRegistry;
import cc.fascinated.fascinatedutils.systems.modules.impl.ChatModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin implements ChatComponentAccess {

    @Shadow
    @Final
    private List<GuiMessage> allMessages;

    @Unique
    private boolean alumite$reentry;

    @Shadow
    protected abstract void refreshTrimmedMessages();

    @Shadow
    protected abstract void addMessage(Component contents, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag tag);

    @Inject(method = "addMessage", at = @At("HEAD"), cancellable = true)
    private void onAddMessage(Component contents, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag tag, CallbackInfo ci) {
        if (alumite$reentry || ChatHistory.INSTANCE.isReplaying()) {
            ChatSearch.INSTANCE.setAddingMessage(true);
            return;
        }

        ChatHistory.INSTANCE.replayIfNewConnection(Minecraft.getInstance());

        ChatMessageEvent event = new ChatMessageEvent(contents, source);
        FascinatedEventBus.INSTANCE.postCancellable(event);
        if (event.isCancelled()) {
            ci.cancel();
            return;
        }

        Optional<ChatModule> moduleOptional = ModuleRegistry.INSTANCE.getModule(ChatModule.class);
        if (moduleOptional.isPresent() && moduleOptional.get().isEnabled()) {
            Component transformed = moduleOptional.get().transform(contents, this);
            if (transformed != contents) {
                alumite$reentry = true;
                try {
                    addMessage(transformed, signature, source, tag);
                } finally {
                    alumite$reentry = false;
                }
                ci.cancel();
                return;
            }
        }
        ChatSearch.INSTANCE.setAddingMessage(true);
    }

    @Inject(method = "addMessage", at = @At("RETURN"))
    private void alumite$afterAddMessage(Component contents, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag tag, CallbackInfo ci) {
        ChatSearch.INSTANCE.setAddingMessage(false);
        if (ChatSearch.INSTANCE.isActive()) {
            refreshTrimmedMessages();
        }
    }

    @Override
    public List<GuiMessage> alumite$getAllMessages() {
        return allMessages;
    }

    @Override
    public void alumite$refreshTrimmedMessages() {
        refreshTrimmedMessages();
    }

    @Override
    public void alumite$addDirect(Component contents) {
        addMessage(contents, null, GuiMessageSource.SYSTEM_CLIENT, null);
    }
}
