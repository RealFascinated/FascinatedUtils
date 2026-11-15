package cc.fascinated.fascinatedutils.systems.turboparticles;

import cc.fascinated.fascinatedutils.common.culling.Cullable;
import net.minecraft.client.particle.SingleQuadParticle;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Skips particles the cull worker has hidden, so the render loop never frustum-tests or extracts them.
 */
public class CulledParticleIterator implements Iterator<SingleQuadParticle> {
    private final Iterator<SingleQuadParticle> delegate;
    private SingleQuadParticle nextParticle;
    private boolean nextReady;

    public CulledParticleIterator(Iterator<SingleQuadParticle> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean hasNext() {
        while (!nextReady && delegate.hasNext()) {
            SingleQuadParticle particle = delegate.next();
            if (!(particle instanceof Cullable cullable) || !cullable.alumite$isCulled()) {
                nextParticle = particle;
                nextReady = true;
            }
        }
        return nextReady;
    }

    @Override
    public SingleQuadParticle next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        SingleQuadParticle particle = nextParticle;
        nextParticle = null;
        nextReady = false;
        return particle;
    }
}
