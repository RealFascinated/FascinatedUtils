package cc.fascinated.fascinatedutils.event.impl;

import lombok.experimental.Accessors;
import net.minecraft.client.player.LocalPlayer;

@Accessors(fluent = true)
public record PlayerDeathEvent(LocalPlayer player) {}
