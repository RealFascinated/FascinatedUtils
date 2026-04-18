package cc.fascinated.fascinatedutils.gui.renderer.packer;

import net.minecraft.resources.Identifier;

/**
 * Packed GUI atlas slot (Meteor-style {@code GuiTexture} placeholder for future atlas-backed chrome).
 *
 * @param identifier texture identifier resolved after packing
 * @param u0         normalized U0
 * @param v0         normalized V0
 * @param u1         normalized U1
 * @param v1         normalized V1
 */
public record GuiTexture(Identifier identifier, float u0, float v0, float u1, float v1) {}
