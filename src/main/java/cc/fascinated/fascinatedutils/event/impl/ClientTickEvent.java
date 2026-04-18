package cc.fascinated.fascinatedutils.event.impl;

import lombok.experimental.Accessors;
import net.minecraft.client.Minecraft;

@Accessors(fluent = true)
public record ClientTickEvent(Minecraft minecraftClient) {}
