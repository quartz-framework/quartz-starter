package xyz.quartzframework.data.storage;

import xyz.quartzframework.data.annotation.SuperStorage;
import xyz.quartzframework.data.interceptor.TransactionCleanupInterceptor;
import xyz.quartzframework.data.interceptor.TransactionalInterceptor;

import java.util.List;

@SuperStorage(value = JPAStorageProvider.class, interceptors = {TransactionalInterceptor.class, TransactionCleanupInterceptor.class})
public interface JPAStorage<E, ID> extends SimpleStorage<E, ID> {

    void flush();

    List<E> saveAndFlush(Iterable<E> entities);

    E saveAndFlush(E entity);

    E merge(E entity);

    void detach(E entity);

    void clear();

    void refresh(E entity);

}