package cc.fascinated.fascinatedutils.debug;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntryStatus;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15C;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GLCapabilities;

import java.util.BitSet;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Measures what the HUD really costs, beyond the time spent building it. The HUD submits its elements inside its own
 * stratum of the vanilla {@code GuiRenderState}, so they form one contiguous range that never merges with vanilla
 * elements. The {@code GuiRenderer} mixin then reports the CPU time spent turning those elements into vertices and the
 * draw range they ended up in, which is bracketed with GL timestamp queries and read a few frames later without
 * stalling. Everything is off unless the Alumite HUD entry is shown in the F3 overlay.
 *
 * <p>States are attributed by identity as they are submitted, and the glyph and blit elements vanilla derives from the
 * HUD's text, items and picture-in-picture states are attributed while they are prepared. Because the HUD's strata
 * hold only HUD states, the sort of their nodes and every draw between the first and last HUD draw are the HUD's. The
 * GPU time of item atlas updates, which render outside the HUD's draw range, is not counted. All methods run on the
 * render thread.
 */
public class HudFrameProfiler {
    public static final HudFrameProfiler INSTANCE = new HudFrameProfiler();

    private static final Identifier ENTRY_ID = Identifier.fromNamespaceAndPath("alumite", "alumite_hud");
    private static final int QUERY_SLOTS = 4;

    private final Set<Object> hudStates = Collections.newSetFromMap(new IdentityHashMap<>());
    private final BitSet hudMeshes = new BitSet();
    private final int[] queryIds = new int[QUERY_SLOTS * 2];
    private final boolean[] queryPending = new boolean[QUERY_SLOTS];
    private boolean measuring;
    private boolean collecting;
    private boolean preparingHud;
    private int recordedMeshes;
    private int firstHudDraw = -1;
    private int lastHudDraw = -1;
    private long guiPassNanos;
    private int writeSlot;
    private int activeSlot = -1;
    private int gpuSupport;
    private boolean queriesCreated;
    /**
     * CPU nanoseconds the HUD spent composing, laying out and submitting last frame.
     */
    @Getter
    private long lastBuildNanos;
    /**
     * CPU nanoseconds vanilla's GUI pass spent building vertices for HUD elements last measured frame.
     */
    @Getter
    private long lastGuiPassNanos;
    /**
     * GPU nanoseconds of the HUD's draw range, a few frames old; negative while no result has arrived.
     */
    @Getter
    private long lastGpuNanos = -1L;

    private HudFrameProfiler() {
    }

    /**
     * Whether the GPU timing is available on this backend.
     *
     * @return true when timestamp queries are supported
     */
    public boolean isGpuSupported() {
        if (gpuSupport == 0) {
            try {
                GLCapabilities capabilities = GL.getCapabilities();
                gpuSupport = capabilities.OpenGL33 || capabilities.GL_ARB_timer_query ? 1 : -1;
            }
            catch (Throwable exception) {
                gpuSupport = -1;
            }
        }
        return gpuSupport > 0;
    }

    /**
     * Whether the current frame is being measured.
     *
     * @return true between {@link #beginFrame} and the end of the GUI pass
     */
    public boolean isMeasuring() {
        return measuring;
    }

    /**
     * Starts a frame's HUD submission. When the Alumite debug entry is shown, a new stratum is opened so the HUD's
     * elements are contiguous, and every element submitted until {@link #endSubmit} is attributed to the HUD.
     *
     * @param renderState the render state the HUD submits into
     * @return true when this frame is measured
     */
    public boolean beginFrame(GuiRenderState renderState) {
        measuring = Minecraft.getInstance().debugEntries.getStatus(ENTRY_ID) != DebugScreenEntryStatus.NEVER;
        hudStates.clear();
        if (measuring) {
            renderState.nextStratum();
            collecting = true;
        }
        return measuring;
    }

    /**
     * Ends the HUD's submission and opens a fresh stratum so later vanilla elements stay out of the HUD's range.
     *
     * @param renderState the render state the HUD submitted into
     * @param buildNanos  CPU nanoseconds the whole submission took
     */
    public void endSubmit(GuiRenderState renderState, long buildNanos) {
        lastBuildNanos = buildNanos;
        if (collecting) {
            collecting = false;
            renderState.nextStratum();
        }
    }

    /**
     * Attributes a render state to the HUD while the HUD is submitting, or while vanilla expands one of the HUD's text,
     * item or picture-in-picture states into glyph and blit elements.
     *
     * @param state the element, text, item or picture-in-picture state being added to the render state
     */
    public void trackState(Object state) {
        if (collecting || preparingHud) {
            hudStates.add(state);
        }
    }

    /**
     * Starts timing vanilla's prepare work for one text, item or picture-in-picture state. Elements it creates are
     * attributed to the HUD when the state is the HUD's.
     *
     * @param state the state vanilla is about to prepare
     * @return a start timestamp to pass to {@link #endStatePrepare}, or 0 when the state is not the HUD's
     */
    public long beginStatePrepare(Object state) {
        if (!measuring || !hudStates.contains(state)) {
            return 0L;
        }
        preparingHud = true;
        return System.nanoTime();
    }

    /**
     * Stops timing a state's prepare work.
     *
     * @param start the value {@link #beginStatePrepare} returned
     */
    public void endStatePrepare(long start) {
        if (start != 0L) {
            preparingHud = false;
            guiPassNanos += System.nanoTime() - start;
        }
    }

    /**
     * Whether an element was submitted by the HUD this frame.
     *
     * @param element the element about to be turned into vertices
     * @return true when the HUD submitted it
     */
    public boolean isHudElement(GuiElementRenderState element) {
        return hudStates.contains(element);
    }

    /**
     * Resets the per-pass mesh bookkeeping when vanilla starts preparing the GUI.
     */
    public void beginPrepare() {
        hudMeshes.clear();
        recordedMeshes = 0;
        firstHudDraw = -1;
        lastHudDraw = -1;
        guiPassNanos = 0L;
    }

    /**
     * Adds CPU time spent preparing HUD elements.
     *
     * @param nanos nanoseconds to add
     */
    public void addGuiPassNanos(long nanos) {
        guiPassNanos += nanos;
    }

    /**
     * Notes the owner of the mesh vanilla just recorded, keeping the mesh list and the draw list aligned.
     *
     * @param meshCount number of meshes recorded so far, which is also the draw count once uploaded
     * @param hud       whether the mesh is made only of HUD elements
     */
    public void onMeshRecorded(int meshCount, boolean hud) {
        if (meshCount > recordedMeshes) {
            recordedMeshes = meshCount;
            hudMeshes.set(meshCount - 1, hud);
        }
    }

    /**
     * Whether the mesh at the index is HUD only.
     *
     * @param meshIndex index into the meshes vanilla uploads
     * @return true when every element of the mesh is from the HUD
     */
    public boolean isHudMesh(int meshIndex) {
        return hudMeshes.get(meshIndex);
    }

    /**
     * Fixes the HUD's draw range once every mesh has been recorded.
     */
    public void onDrawsRecorded() {
        firstHudDraw = hudMeshes.nextSetBit(0);
        lastHudDraw = hudMeshes.length() - 1;
    }

    /**
     * Starts the GPU timestamp when vanilla is about to execute the HUD's first draw.
     *
     * @param drawIndex index of the draw about to run
     */
    public void onDrawStart(int drawIndex) {
        if (drawIndex != firstHudDraw || !isGpuSupported() || queryPending[writeSlot]) {
            return;
        }
        if (!queriesCreated) {
            for (int index = 0; index < queryIds.length; index++) {
                queryIds[index] = GL15C.glGenQueries();
            }
            queriesCreated = true;
        }
        activeSlot = writeSlot;
        GL33C.glQueryCounter(queryIds[activeSlot * 2], GL33C.GL_TIMESTAMP);
    }

    /**
     * Ends the GPU timestamp after vanilla executed the HUD's last draw.
     *
     * @param drawIndex index of the draw that just ran
     */
    public void onDrawEnd(int drawIndex) {
        if (drawIndex != lastHudDraw || activeSlot < 0) {
            return;
        }
        GL33C.glQueryCounter(queryIds[activeSlot * 2 + 1], GL33C.GL_TIMESTAMP);
        queryPending[activeSlot] = true;
        writeSlot = (writeSlot + 1) % QUERY_SLOTS;
        activeSlot = -1;
    }

    /**
     * Publishes the frame's figures once vanilla finished the GUI pass and collects any finished GPU timings.
     */
    public void endGuiPass() {
        if (!measuring) {
            return;
        }
        measuring = false;
        lastGuiPassNanos = guiPassNanos;
        hudStates.clear();
        if (!isGpuSupported()) {
            return;
        }
        activeSlot = -1;
        if (firstHudDraw < 0) {
            lastGpuNanos = 0L;
        }
        for (int offset = 0; offset < QUERY_SLOTS; offset++) {
            int slot = (writeSlot + offset) % QUERY_SLOTS;
            if (!queryPending[slot] || GL15C.glGetQueryObjecti(queryIds[slot * 2 + 1], GL15C.GL_QUERY_RESULT_AVAILABLE) == 0) {
                continue;
            }
            long begin = GL33C.glGetQueryObjecti64(queryIds[slot * 2], GL15C.GL_QUERY_RESULT);
            long end = GL33C.glGetQueryObjecti64(queryIds[slot * 2 + 1], GL15C.GL_QUERY_RESULT);
            lastGpuNanos = Math.max(0L, end - begin);
            queryPending[slot] = false;
        }
    }
}
