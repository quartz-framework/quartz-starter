package xyz.quartzframework.data.specification;

import xyz.quartzframework.data.page.Page;
import xyz.quartzframework.data.page.Pagination;
import xyz.quartzframework.data.page.Sort;

import java.util.List;

public interface QuerySpecificationExecutor<E> {

    List<E> find(QuerySpecification<E> spec);

    List<E> find(QuerySpecification<E> spec, Sort sort);

    Page<E> find(QuerySpecification<E> spec, Pagination pagination);

    long count(QuerySpecification<E> spec);

    boolean exists(QuerySpecification<E> spec);

}