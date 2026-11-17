package cc.fascinated.fascinatedutils.systems.modules.impl.wawla;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.entity.Entity;

import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class WawlaEntityExtension<E extends Entity> {
    private final Class<E> entityClass;

    public boolean matches(Entity entity) {
        return entityClass.isInstance(entity);
    }

    @SuppressWarnings("unchecked")
    public final List<String> apply(Entity entity) {
        return getExtension((E) entity);
    }

    public abstract List<String> getExtension(E entity);
}
