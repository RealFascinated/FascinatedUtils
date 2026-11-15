package cc.fascinated.fascinatedutils.common.culling;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Hand-off of a list snapshot from the main thread to the cull thread that reuses two backing lists instead of
 * allocating a new snapshot per publish.
 *
 * <p>The main thread fills a list that is neither published nor held by the cull thread, then publishes it with a
 * volatile write. The cull thread marks the list it reads as in use (and re-checks that it is still the published one)
 * before iterating, and releases it afterwards. The main thread never mutates a published list or one marked in use; if
 * both backing lists are unavailable it falls back to a fresh list, so a slow cull pass can never observe a mutation.
 *
 * @param <T> element type
 */
public class CullSnapshotBuffers<T> {
    private final ArrayList<T> firstBuffer = new ArrayList<>();
    private final ArrayList<T> secondBuffer = new ArrayList<>();
    private volatile List<T> published = List.of();
    private volatile List<T> inUse = null;
    @Getter
    private volatile long publishedNanos = 0L;

    /**
     * Cull thread: returns the published snapshot and marks it as in use until {@link #release()}.
     *
     * @return the published snapshot, safe to iterate until released
     */
    public List<T> acquire() {
        while (true) {
            List<T> current = published;
            inUse = current;
            if (published == current) {
                return current;
            }
        }
    }

    /**
     * Cull thread: marks the snapshot returned by {@link #acquire()} as no longer read.
     */
    public void release() {
        inUse = null;
    }

    /**
     * Main thread: returns an empty list to fill for the next publish. It is never the published list or the one the
     * cull thread is reading.
     *
     * @return an empty list owned by the caller until {@link #publish(List)}
     */
    public ArrayList<T> beginPublish() {
        List<T> current = published;
        List<T> reading = inUse;
        ArrayList<T> target;
        if (current != firstBuffer && reading != firstBuffer) {
            target = firstBuffer;
        }
        else if (current != secondBuffer && reading != secondBuffer) {
            target = secondBuffer;
        }
        else {
            return new ArrayList<>();
        }
        target.clear();
        return target;
    }

    /**
     * Main thread: makes a list obtained from {@link #beginPublish()} the snapshot the cull thread reads.
     *
     * @param snapshot the filled list, which the caller must no longer modify
     */
    public void publish(List<T> snapshot) {
        publishedNanos = System.nanoTime();
        published = snapshot;
    }

    /**
     * Main thread: publishes an empty snapshot and drops the references held by the backing lists that the cull
     * thread is not reading.
     */
    public void clear() {
        publishedNanos = System.nanoTime();
        published = List.of();
        List<T> reading = inUse;
        if (reading != firstBuffer) {
            firstBuffer.clear();
        }
        if (reading != secondBuffer) {
            secondBuffer.clear();
        }
    }
}
