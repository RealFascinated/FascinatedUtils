package cc.fascinated.fascinatedutils.event.impl.lifecycle;

import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;

@Accessors(fluent = true)
public record ClientStartedEvent(Minecraft minecraftClient) {}
