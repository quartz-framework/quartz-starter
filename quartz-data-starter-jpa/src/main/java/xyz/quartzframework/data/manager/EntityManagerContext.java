package xyz.quartzframework.data.manager;

import jakarta.persistence.EntityManager;
import org.springframework.lang.Nullable;

public class EntityManagerContext {

    private static final ThreadLocal<EntityManager> context = new ThreadLocal<>();

    public static void set(EntityManager em) {
        context.set(em);
    }

    @Nullable
    public static EntityManager get() {
        return context.get();
    }

    public static void clear() {
        context.remove();
    }
}