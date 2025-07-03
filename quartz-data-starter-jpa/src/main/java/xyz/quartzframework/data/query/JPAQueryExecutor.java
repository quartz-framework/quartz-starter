package xyz.quartzframework.data.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.criteria.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import xyz.quartzframework.data.page.Page;
import xyz.quartzframework.data.page.Pagination;
import xyz.quartzframework.data.util.ParameterBindingUtil;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class JPAQueryExecutor<E> implements QueryExecutor<E> {

    private final EntityManagerFactory entityManagerFactory;

    private final Class<E> entityClass;

    @Override
    public List<E> find(DynamicQueryDefinition query, Object[] args) {
        try (val em = entityManagerFactory.createEntityManager()) {
            if (query.nativeSQL() && query.raw() != null) {
                return executeNativeQuery(em, query, args);
            }
            val jpaQuery = buildJpaQuery(em, query, args);
            if (query.limit() != null) jpaQuery.setMaxResults(query.limit());
            return jpaQuery.getResultList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<E> find(DynamicQueryDefinition query, Object[] args, Pagination pagination) {
        try (val em = entityManagerFactory.createEntityManager()) {
            if (query.nativeSQL() && query.raw() != null) {
                val nativeQuery = em.createNativeQuery(query.raw(), entityClass);
                bindParameters(nativeQuery, query, args);
                nativeQuery.setFirstResult(pagination.offset());
                nativeQuery.setMaxResults(pagination.size());
                List<E> items = nativeQuery.getResultList();
                long total = count(query, args);
                return Page.of(items, pagination, (int) total);
            }
            val jpaQuery = buildJpaQuery(em, query, args);
            jpaQuery.setFirstResult(pagination.offset());
            jpaQuery.setMaxResults(pagination.size());
            List<E> items = jpaQuery.getResultList();
            long total = count(query, args);
            return Page.of(items, pagination, (int) total);
        }
    }

    @Override
    public long count(DynamicQueryDefinition query, Object[] args) {
        try (val em = entityManagerFactory.createEntityManager()) {
            if (query.nativeSQL() && query.raw() != null) {
                val nativeQuery = em.createNativeQuery(query.raw());
                bindParameters(nativeQuery, query, args);
                Object result = nativeQuery.getSingleResult();
                return ((Number) result).longValue();
            }
            val cb = em.getCriteriaBuilder();
            val cq = cb.createQuery(Long.class);
            val root = cq.from(entityClass);
            cq.select(cb.count(root)).where(buildPredicates(query, cb, root, query.conditions(), args));
            return em.createQuery(cq).getSingleResult();
        }
    }

    @Override
    public boolean exists(DynamicQueryDefinition query, Object[] args) {
        try (val em = entityManagerFactory.createEntityManager()) {
            if (query.nativeSQL() && query.raw() != null) {
                val nativeQuery = em.createNativeQuery(query.raw());
                bindParameters(nativeQuery, query, args);
                Object result = nativeQuery.getSingleResult();
                return ((Number) result).intValue() > 0;
            }
            val cb = em.getCriteriaBuilder();
            val cq = cb.createQuery(Long.class);
            val root = cq.from(entityClass);
            cq.select(cb.count(root)).where(buildPredicates(query, cb, root, query.conditions(), args));
            Long count = em.createQuery(cq).setMaxResults(1).getSingleResult();
            return count != null && count > 0;
        }
    }

    @SuppressWarnings("unchecked")
    private List<E> executeNativeQuery(EntityManager em, DynamicQueryDefinition query, Object[] args) {
        val sql = query.raw();
        val nativeQuery = switch (query.action()) {
            case COUNT, EXISTS -> em.createNativeQuery(sql);
            default -> em.createNativeQuery(sql, entityClass);
        };
        bindParameters(nativeQuery, query, args);

        if (query.action() == QueryAction.COUNT) {
            Object rawResult = nativeQuery.getSingleResult();
            long count = ((Number) rawResult).longValue();
            return List.of((E) Long.valueOf(count));
        }
        if (query.action() == QueryAction.EXISTS) {
            Object rawResult = nativeQuery.getSingleResult();
            boolean exists = ((Number) rawResult).intValue() > 0;
            return List.of((E) Boolean.valueOf(exists));
        }
        if (query.limit() != null) nativeQuery.setMaxResults(query.limit());
        return nativeQuery.getResultList();
    }

    private TypedQuery<E> buildJpaQuery(EntityManager em, DynamicQueryDefinition query, Object[] args) {
        val cb = em.getCriteriaBuilder();
        val cq = cb.createQuery(entityClass);
        val root = cq.from(entityClass);
        cq.where(buildPredicates(query, cb, root, query.conditions(), args));
        if (!query.orders().isEmpty()) {
            cq.orderBy(buildOrders(cb, root, query.orders()));
        }
        return em.createQuery(cq);
    }

    private Predicate[] buildPredicates(DynamicQueryDefinition query, CriteriaBuilder cb, Root<E> root, List<Condition> conditions, Object[] args) {
        return conditions.stream()
                .map(cond -> buildPredicate(query, cond, cb, root, args))
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new);
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<E> root, List<xyz.quartzframework.data.query.Order> orders) {
        return orders.stream()
                .map(order -> order.descending()
                        ? cb.desc(root.get(order.property()))
                        : cb.asc(root.get(order.property())))
                .toList();
    }

    private void bindParameters(jakarta.persistence.Query query, DynamicQueryDefinition def, Object[] args) {
        for (Condition cond : def.conditions()) {
            Object value;

            if (cond.namedParameter() != null) {
                value = ParameterBindingUtil.findNamedParameter(def.method(), cond.namedParameter(), args);
                query.setParameter(cond.namedParameter(), value);
            } else if (cond.paramIndex() != null) {
                value = args[cond.paramIndex()];
                query.setParameter(cond.paramIndex() + 1, value);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    private Predicate buildPredicate(DynamicQueryDefinition query, Condition cond, CriteriaBuilder cb, Root<E> root, Object[] args) {
        Object value;
        if (cond.fixedValue() != null || cond.operation() == Operation.IS_NULL || cond.operation() == Operation.IS_NOT_NULL) {
            value = cond.fixedValue();
        } else if (cond.namedParameter() != null) {
            value = ParameterBindingUtil.findNamedParameter(query.method(), cond.namedParameter(), args);
        } else if (cond.paramIndex() != null) {
            value = args[cond.paramIndex()];
        } else {
            throw new IllegalStateException("No param index, named parameter, or fixed value for condition: " + cond);
        }

        Path<?> path = resolvePath(root, cond.property());

        return switch (cond.operation()) {
            case EQUAL -> cb.equal(path, value);
            case NOT_EQUAL -> cb.notEqual(path, value);
            case LIKE -> cb.like(path.as(String.class), value.toString());
            case NOT_LIKE -> cb.notLike(path.as(String.class), value.toString());
            case IS_NULL -> cb.isNull(path);
            case IS_NOT_NULL -> cb.isNotNull(path);
            case IN -> path.in((Collection<?>) value);
            case NOT_IN -> cb.not(path.in((Collection<?>) value));
            case GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL -> {
                @SuppressWarnings("unchecked")
                Path<? extends Comparable<Object>> comparablePath = (Path<? extends Comparable<Object>>) path;
                @SuppressWarnings("unchecked")
                Comparable<Object> comparableValue = (Comparable<Object>) value;
                yield switch (cond.operation()) {
                    case GREATER_THAN -> cb.greaterThan(comparablePath, comparableValue);
                    case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(comparablePath, comparableValue);
                    case LESS_THAN -> cb.lessThan(comparablePath, comparableValue);
                    case LESS_THAN_OR_EQUAL -> cb.lessThanOrEqualTo(comparablePath, comparableValue);
                    default -> throw new IllegalArgumentException("Unexpected operation: " + cond.operation());
                };
            }
        };
    }

    @SuppressWarnings("unchecked")
    private Path<Object> resolvePath(From<?, ?> root, String propertyPath) {
        String[] parts = propertyPath.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return (Path<Object>) path;
    }
}