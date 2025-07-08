package xyz.quartzframework.data.interceptor;

import lombok.NonNull;
import lombok.val;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.lang.reflect.Method;

public class TransactionalInterceptor implements MethodInterceptor {

    private final boolean disabled;

    private final PlatformTransactionManager txManager;

    public TransactionalInterceptor(PlatformTransactionManager txManager, boolean disabled) {
        this.txManager = txManager;
        this.disabled = disabled;
    }

    @Override
    public Object invoke(@NonNull MethodInvocation invocation) throws Throwable {
        if (disabled) return invocation.proceed();
        Method method = invocation.getMethod();
        Transactional transactional = resolveTransactionalAnnotation(method, invocation.getThis());
        if (transactional == null) {
            return invocation.proceed();
        }
        val def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(resolvePropagation(transactional.propagation().name()));
        def.setIsolationLevel(resolveIsolation(transactional.isolation().name()));
        def.setTimeout(transactional.timeout());
        def.setReadOnly(transactional.readOnly());
        TransactionStatus status = txManager.getTransaction(def);
        try {
            Object result = invocation.proceed();
            txManager.commit(status);
            return result;
        } catch (Throwable ex) {
            if (shouldRollbackFor(transactional, ex)) {
                txManager.rollback(status);
            } else {
                txManager.commit(status);
            }
            throw ex;
        }
    }

    private Transactional resolveTransactionalAnnotation(Method method, Object target) {
        if (method.isAnnotationPresent(Transactional.class)) {
            return method.getAnnotation(Transactional.class);
        }
        if (target != null) {
            Class<?> clazz = target.getClass();
            if (clazz.isAnnotationPresent(Transactional.class)) {
                return clazz.getAnnotation(Transactional.class);
            }
        }
        return null;
    }

    private int resolvePropagation(String name) {
        return switch (name) {
            case "REQUIRES_NEW" -> TransactionDefinition.PROPAGATION_REQUIRES_NEW;
            case "NESTED" -> TransactionDefinition.PROPAGATION_NESTED;
            case "MANDATORY" -> TransactionDefinition.PROPAGATION_MANDATORY;
            case "NEVER" -> TransactionDefinition.PROPAGATION_NEVER;
            case "NOT_SUPPORTED" -> TransactionDefinition.PROPAGATION_NOT_SUPPORTED;
            default -> TransactionDefinition.PROPAGATION_REQUIRED;
        };
    }

    private int resolveIsolation(String name) {
        return switch (name) {
            case "READ_COMMITTED" -> TransactionDefinition.ISOLATION_READ_COMMITTED;
            case "READ_UNCOMMITTED" -> TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
            case "REPEATABLE_READ" -> TransactionDefinition.ISOLATION_REPEATABLE_READ;
            case "SERIALIZABLE" -> TransactionDefinition.ISOLATION_SERIALIZABLE;
            default -> TransactionDefinition.ISOLATION_DEFAULT;
        };
    }

    private boolean shouldRollbackFor(Transactional transactional, Throwable ex) {
        for (Class<? extends Throwable> rollbackClass : transactional.rollbackFor()) {
            if (rollbackClass.isAssignableFrom(ex.getClass())) {
                return true;
            }
        }
        for (Class<? extends Throwable> noRollbackClass : transactional.noRollbackFor()) {
            if (noRollbackClass.isAssignableFrom(ex.getClass())) {
                return false;
            }
        }
        return true;
    }
}