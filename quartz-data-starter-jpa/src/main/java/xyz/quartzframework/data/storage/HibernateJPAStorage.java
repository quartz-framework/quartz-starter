package xyz.quartzframework.data.storage;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import xyz.quartzframework.data.manager.EntityManagerContext;
import xyz.quartzframework.data.page.Page;
import xyz.quartzframework.data.page.Pagination;
import xyz.quartzframework.data.page.Sort;
import xyz.quartzframework.data.specification.QuerySpecification;
import xyz.quartzframework.data.specification.QuerySpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class HibernateJPAStorage<E, ID> implements JPAStorage<E, ID>, QuerySpecificationExecutor<E> {

    private final EntityManagerFactory entityManagerFactory;

    @Getter
    private final Class<E> entityClass;

    @Getter
    private final Class<ID> idClass;

    @Override
    public Optional<E> findById(ID id) {
        return execute(em -> Optional.ofNullable(em.find(entityClass, id)));
    }

    @Override
    public long count() {
        return execute(em -> {
            String jpql = "select count(e) from " + entityClass.getSimpleName() + " e";
            return em.createQuery(jpql, Long.class).getSingleResult();
        });
    }

    @Override
    public boolean exists(ID id) {
        return execute(em -> em.find(entityClass, id) != null);
    }

    @Override
    public E save(E entity) {
        return executeInTransaction(em -> {
            em.merge(entity);
            return entity;
        });
    }

    @Override
    public List<E> save(Iterable<E> entities) {
        return executeInTransaction(em -> {
            List<E> saved = new ArrayList<>();
            int i = 0;
            for (E entity : entities) {
                em.merge(entity);
                saved.add(entity);
                if (++i % 50 == 0) em.flush();
            }
            return saved;
        });
    }

    @Override
    public void deleteById(ID id) {
        executeInTransaction(em -> {
            E entity = em.find(entityClass, id);
            if (entity != null) em.remove(entity);
            return null;
        });
    }

    @Override
    public void delete(E entity) {
        executeInTransaction(em -> {
            E managed = em.contains(entity) ? entity : em.merge(entity);
            em.remove(managed);
            return null;
        });
    }

    @Override
    public void delete(Iterable<E> entities) {
        executeInTransaction(em -> {
            int i = 0;
            for (E entity : entities) {
                E managed = em.contains(entity) ? entity : em.merge(entity);
                em.remove(managed);
                if (++i % 50 == 0) {
                    em.flush();
                    em.clear();
                }
            }
            return null;
        });
    }

    @Override
    public List<E> findAll() {
        return execute(em -> {
            String jpql = "from " + entityClass.getSimpleName();
            return em.createQuery(jpql, entityClass).getResultList();
        });
    }

    @Override
    public List<E> findAll(Sort sort) {
        return execute(em -> {
            StringBuilder jpql = new StringBuilder("from " + entityClass.getSimpleName());
            if (sort.isSorted()) {
                jpql.append(" order by ");
                jpql.append(String.join(", ", sort.getOrders().stream()
                        .map(order -> "e." + order.property() + (order.direction().equals(Sort.Direction.DESC) ? " desc" : " asc"))
                        .toList()));
            }
            return em.createQuery(jpql.toString(), entityClass).getResultList();
        });
    }

    @Override
    public Page<E> findAll(Pagination pagination) {
        return execute(em -> {
            String jpql = "from " + entityClass.getSimpleName();
            val query = em.createQuery(jpql, entityClass);
            query.setFirstResult((pagination.page() - 1) * pagination.size());
            query.setMaxResults(pagination.size());
            List<E> content = query.getResultList();
            long total = count();
            return Page.of(content, pagination.page(), pagination.size(), total);
        });
    }

    @Override
    public void flush() {
        executeInTransaction(em -> {
            em.flush();
            return null;
        });
    }

    @Override
    public List<E> saveAndFlush(Iterable<E> entities) {
        return executeInTransaction(em -> {
            List<E> saved = new ArrayList<>();
            for (E entity : entities) {
                em.merge(entity);
                saved.add(entity);
            }
            em.flush();
            return saved;
        });
    }

    @Override
    public E saveAndFlush(E entity) {
        return executeInTransaction(em -> {
            em.merge(entity);
            em.flush();
            return entity;
        });
    }

    @Override
    public List<E> find(QuerySpecification<E> spec) {
        return execute(em -> {
            val cb = em.getCriteriaBuilder();
            val query = cb.createQuery(entityClass);
            val root = query.from(entityClass);
            val predicate = spec.toPredicate(root, query, cb);
            query.where(predicate);
            return em.createQuery(query).getResultList();
        });
    }

    @Override
    public List<E> find(QuerySpecification<E> spec, Sort sort) {
        return execute(em -> {
            val cb = em.getCriteriaBuilder();
            val query = cb.createQuery(entityClass);
            val root = query.from(entityClass);
            val predicate = spec.toPredicate(root, query, cb);
            query.where(predicate);
            if (sort.isSorted()) {
                query.orderBy(sort.getOrders().stream()
                        .map(order -> order.direction().equals(Sort.Direction.DESC)
                                ? cb.desc(root.get(order.property()))
                                : cb.asc(root.get(order.property())))
                        .toList());
            }
            return em.createQuery(query).getResultList();
        });
    }

    @Override
    public Page<E> find(QuerySpecification<E> spec, Pagination pagination) {
        return execute(em -> {
            val cb = em.getCriteriaBuilder();
            val query = cb.createQuery(entityClass);
            val root = query.from(entityClass);
            val predicate = spec.toPredicate(root, query, cb);
            query.where(predicate);
            val jpaQuery = em.createQuery(query);
            jpaQuery.setFirstResult((pagination.page() - 1) * pagination.size());
            jpaQuery.setMaxResults(pagination.size());
            List<E> content = jpaQuery.getResultList();

            val countQuery = cb.createQuery(Long.class);
            val countRoot = countQuery.from(entityClass);
            countQuery.select(cb.count(countRoot)).where(spec.toPredicate(countRoot, countQuery, cb));
            long total = em.createQuery(countQuery).getSingleResult();
            return Page.of(content, pagination.page(), pagination.size(), total);
        });
    }

    @Override
    public long count(QuerySpecification<E> spec) {
        return execute(em -> {
            val cb = em.getCriteriaBuilder();
            val query = cb.createQuery(Long.class);
            val root = query.from(entityClass);
            query.select(cb.count(root)).where(spec.toPredicate(root, query, cb));
            return em.createQuery(query).getSingleResult();
        });
    }

    @Override
    public boolean exists(QuerySpecification<E> spec) {
        return execute(em -> {
            val cb = em.getCriteriaBuilder();
            val query = cb.createQuery(Long.class);
            val root = query.from(entityClass);
            query.select(cb.count(root)).where(spec.toPredicate(root, query, cb));
            return em.createQuery(query).setMaxResults(1).getSingleResult() > 0;
        });
    }

    private EntityManager getEntityManager() {
        EntityManager em = EntityManagerContext.get();
        if (em != null && em.isOpen()) {
            return em;
        }
        return entityManagerFactory.createEntityManager();
    }

    private boolean outOfTransactionalEntityManager(EntityManager em) {
        EntityManager ctx = EntityManagerContext.get();
        return ctx == null || ctx != em;
    }

    private <T> T execute(EntityManagerCallback<T> callback) {
        val em = getEntityManager();
        try {
            return callback.call(em);
        } finally {
            if (outOfTransactionalEntityManager(em) && em.isOpen()) em.close();
        }
    }

    private <T> T executeInTransaction(EntityManagerCallback<T> callback) {
        val em = getEntityManager();
        val tx = em.getTransaction();
        boolean newTransaction = !tx.isActive();
        if (newTransaction) tx.begin();
        try {
            T result = callback.call(em);
            if (newTransaction) tx.commit();
            return result;
        } catch (Exception e) {
            if (newTransaction && tx.isActive()) tx.rollback();
            throw new RuntimeException(e);
        } finally {
            if (outOfTransactionalEntityManager(em) && em.isOpen()) em.close();
        }
    }

    private interface EntityManagerCallback<T> {

        T call(EntityManager em);

    }
}