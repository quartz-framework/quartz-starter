package xyz.quartzframework.data.interceptor;

import jakarta.persistence.EntityManager;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.lang.NonNull;
import xyz.quartzframework.data.manager.EntityManagerContext;

public class TransactionCleanupInterceptor implements MethodInterceptor {

    private final boolean disabled;

    public TransactionCleanupInterceptor(boolean disabled) {
        this.disabled = disabled;
    }

    @Override
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        if (disabled) return invocation.proceed();
        try {
            return invocation.proceed();
        } finally {
            EntityManager em = EntityManagerContext.get();
            if (em != null && em.isOpen()) {
                em.close();
            }
            EntityManagerContext.clear();
        }
    }
}