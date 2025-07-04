package xyz.quartzframework.data.configurer;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.interceptor.TransactionCleanupInterceptor;
import xyz.quartzframework.data.interceptor.TransactionalInterceptor;
import xyz.quartzframework.data.manager.DefaultJPATransactionManager;

@Slf4j
@Configurer(force = true)
@RequiredArgsConstructor
public class TransactionalSupportConfigurer {

    private final PluginBeanFactory pluginBeanFactory;

    @Provide
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new DefaultJPATransactionManager(emf);
    }

    @Provide
    TransactionalInterceptor transactionInterceptor(PlatformTransactionManager transactionManager) {
        return new TransactionalInterceptor(transactionManager, pluginBeanFactory);
    }

    @Provide
    TransactionCleanupInterceptor transactionInterceptor() {
        return new TransactionCleanupInterceptor(pluginBeanFactory);
    }
}