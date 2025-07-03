package xyz.quartzframework.data.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@FunctionalInterface
public interface QuerySpecification<T> {

    Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder cb);

}