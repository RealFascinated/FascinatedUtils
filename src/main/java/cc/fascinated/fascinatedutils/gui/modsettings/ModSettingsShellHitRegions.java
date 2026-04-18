package cc.fascinated.fascinatedutils.gui.modsettings;

/**
 * Snapshot of shell regions in layout space for hit-testing between frames.
 *
 * <p>Bounds use {@link ModSettingsShellLayout.ShellBounds} semantics for containment checks.
 *
 * @param close         close control bounds
 * @param topBarTabs    tab strip host bounds
 * @param hudLayoutChip HUD layout editor chip bounds
 * @param bodyPositionX body area left edge in layout space
 * @param bodyTop       body area top edge in layout space
 * @param bodyWidth     body area width in layout space
 * @param bodyHeight    body area height in layout space
 */
public record ModSettingsShellHitRegions(ModSettingsShellLayout.ShellBounds close,
                                         ModSettingsShellLayout.ShellBounds topBarTabs,
                                         ModSettingsShellLayout.ShellBounds hudLayoutChip, float bodyPositionX,
                                         float bodyTop, float bodyWidth, float bodyHeight) {}
