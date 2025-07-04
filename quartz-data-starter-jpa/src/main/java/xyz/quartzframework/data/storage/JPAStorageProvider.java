package xyz.quartzframework.data.storage;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.val;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.data.query.JPAQueryExecutor;
import xyz.quartzframework.data.query.QueryExecutor;
import xyz.quartzframework.data.util.GenericTypeUtil;

@Injectable
@RequiredArgsConstructor
@ActivateWhenBeanMissing(JPAStorageProvider.class)
public class JPAStorageProvider implements StorageProvider {

    private final EntityManagerFactory entityManagerFactory;

    @Override
    public <E, ID> HibernateJPAStorage<E, ID> create(Class<E> entity, Class<ID> id) {
        return new HibernateJPAStorage<>(entityManagerFactory, entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E, ID> QueryExecutor<E> getQueryExecutor(SimpleStorage<E, ID> storage) {
        val storageInterface = storage.getClass();
        Class<?>[] types = GenericTypeUtil.resolve(storageInterface, SimpleStorage.class);
        if (types == null || types.length != 2) {
            throw new IllegalArgumentException(storageInterface.getName() + " is not a supported storage interface");
        }
        return new JPAQueryExecutor<>(entityManagerFactory, (Class<E>) types[0]);
    }
}