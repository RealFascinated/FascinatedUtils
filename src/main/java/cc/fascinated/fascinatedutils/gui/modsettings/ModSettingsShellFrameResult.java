package cc.fascinated.fascinatedutils.gui.modsettings;

/**
 * Outcome of a mod settings shell paint pass.
 *
 * <p>Carries hit regions for input routing and the pointer position in shell layout space used for
 * that frame's rendering.
 *
 * @param hitRegions     regions for hit-testing until the next frame
 * @param pointerLayoutX pointer horizontal position in shell layout space
 * @param pointerLayoutY pointer vertical position in shell layout space
 */
public record ModSettingsShellFrameResult(ModSettingsShellHitRegions hitRegions, float pointerLayoutX,
                                          float pointerLayoutY) {}
