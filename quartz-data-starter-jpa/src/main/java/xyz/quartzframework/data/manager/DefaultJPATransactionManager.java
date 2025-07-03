package xyz.quartzframework.data.manager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionStatus;

@RequiredArgsConstructor
public class DefaultJPATransactionManager implements PlatformTransactionManager {

    private final EntityManagerFactory emf;

    @Override
    public TransactionStatus getTransaction(@Nullable TransactionDefinition definition) {
        EntityManager em = emf.createEntityManager();
        em.getTransaction().begin();
        EntityManagerContext.set(em);
        return new DefaultTransactionStatus(
                null,
                em,
                true,
                true,
                false,
                definition != null && definition.isReadOnly(),
                false,
                null
        );
    }

    @Override
    public void commit(TransactionStatus status) {
        EntityManager em = (EntityManager) ((DefaultTransactionStatus) status).getTransaction();
        em.getTransaction().commit();
    }

    @Override
    public void rollback(TransactionStatus status) {
        EntityManager em = (EntityManager) ((DefaultTransactionStatus) status).getTransaction();
        em.getTransaction().rollback();
    }
}