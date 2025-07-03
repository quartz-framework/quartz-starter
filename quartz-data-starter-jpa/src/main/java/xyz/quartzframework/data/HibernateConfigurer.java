package xyz.quartzframework.data;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
import org.springframework.transaction.PlatformTransactionManager;
import xyz.quartzframework.core.bean.annotation.Priority;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.helper.AutoDialectHelper;
import xyz.quartzframework.data.helper.DataSourceBuilder;
import xyz.quartzframework.data.interceptor.TransactionCleanupInterceptor;
import xyz.quartzframework.data.interceptor.TransactionalInterceptor;
import xyz.quartzframework.data.manager.DefaultJPATransactionManager;
import xyz.quartzframework.data.properties.HibernateProperties;
import xyz.quartzframework.data.properties.HikariProperties;
import xyz.quartzframework.data.properties.JPAPersistenceProperties;
import xyz.quartzframework.data.storage.StorageRegistrar;

import javax.sql.DataSource;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configurer(force = true)
@RequiredArgsConstructor
public class HibernateConfigurer {

    private final PluginBeanFactory pluginBeanFactory;

    private final URLClassLoader classLoader;

    private final HibernateProperties hibernateProperties;

    private final HikariProperties hikariProperties;

    private final JPAPersistenceProperties jpaProperties;

    @Provide
    @Priority(0)
    @ActivateWhenBeanMissing(DataSource.class)
    public DataSource dataSource() {
        return DataSourceBuilder.build(jpaProperties, hikariProperties, classLoader);
    }

    @Provide
    @Priority(1)
    @ActivateWhenBeanMissing(StandardServiceRegistry.class)
    StandardServiceRegistry standardServiceRegistry(DataSource dataSource) {
        Map<String, Object> settings = new HashMap<>();
        settings.put(Environment.JAKARTA_JDBC_URL, jpaProperties.getDatasourceUrl());
        settings.put(Environment.JAKARTA_JDBC_USER, jpaProperties.getDatasourceUsername());
        settings.put(Environment.JAKARTA_JDBC_PASSWORD, jpaProperties.getDatasourcePassword());
        String dialect = hibernateProperties.getDialect();
        if (dialect == null || dialect.isBlank()) {
            dialect = AutoDialectHelper.resolveDialect(classLoader);
            if (dialect == null) {
                throw new IllegalStateException("Could not determine Hibernate dialect: no known JDBC driver found on classpath.");
            }
            log.info("Auto-detected Hibernate dialect: {}", dialect);
        }
        settings.put(Environment.DIALECT, dialect);
        val jtaDisabled = pluginBeanFactory.getBeansWithAnnotation(EnableTransactionalSupport.class).isEmpty();
        if (jtaDisabled) {
            settings.put(Environment.JAKARTA_NON_JTA_DATASOURCE, dataSource);
        } else {
            settings.put(Environment.JAKARTA_JTA_DATASOURCE, dataSource);
        }
        settings.put(Environment.HBM2DDL_AUTO, hibernateProperties.getDdlAuto());
        settings.put(Environment.SHOW_SQL, hibernateProperties.isShowSql());
        settings.put(Environment.FORMAT_SQL, hibernateProperties.isFormatSql());
        settings.put(Environment.HIGHLIGHT_SQL, hibernateProperties.isHighlightSql());
        settings.put(Environment.LOG_SLOW_QUERY, hibernateProperties.isLogSlowQuery());
        settings.put(Environment.USE_SQL_COMMENTS, hibernateProperties.isUseSqlComments());
        settings.put(Environment.STATEMENT_FETCH_SIZE, hibernateProperties.getFetchSize());
        settings.put(Environment.USE_SCROLLABLE_RESULTSET, hibernateProperties.isUseScrollableResultSet());
        settings.put(Environment.NON_CONTEXTUAL_LOB_CREATION, hibernateProperties.isNonContextualLobCreation());
        settings.put(Environment.LOG_JDBC_WARNINGS, hibernateProperties.isLogJdbcWarnings());
        settings.put(Environment.USE_GET_GENERATED_KEYS, hibernateProperties.isUseGetGeneratedKeys());
        settings.put(Environment.DIALECT_NATIVE_PARAM_MARKERS, hibernateProperties.isDialectNativeParamMarkers());
        if (hibernateProperties.getJdbcTimeZone() != null && !hibernateProperties.getJdbcTimeZone().isBlank()) {
            settings.put(Environment.JDBC_TIME_ZONE, hibernateProperties.getJdbcTimeZone());
        }
        if (hibernateProperties.getConnectionHandling() != null && !hibernateProperties.getConnectionHandling().isBlank()) {
            settings.put(Environment.CONNECTION_HANDLING, hibernateProperties.getConnectionHandling());
        }
        if (hibernateProperties.getStatementInspector() != null && !hibernateProperties.getStatementInspector().isBlank()) {
            settings.put(Environment.STATEMENT_INSPECTOR, hibernateProperties.getStatementInspector());
        }
        settings.put(Environment.POOL_SIZE, jpaProperties.getConnectionPoolSize());
        settings.put(Environment.ISOLATION, jpaProperties.getConnectionIsolation());
        settings.put(Environment.AUTOCOMMIT, jpaProperties.isConnectionAutocommit());
        settings.put(Environment.CONNECTION_PROVIDER_DISABLES_AUTOCOMMIT, jpaProperties.isProviderDisablesAutocommit());
        val builder = new StandardServiceRegistryBuilder();
        builder.applySettings(settings);
        return builder.build();
    }

    @Provide
    @Priority(2)
    @ActivateWhenBeanMissing(MetadataSources.class)
    MetadataSources metadataSources(StandardServiceRegistry registry) {
        return new MetadataSources(registry);
    }

    @Provide
    @Priority(3)
    @ActivateWhenBeanMissing(SessionFactory.class)
    SessionFactory sessionFactory(MetadataSources sources, StorageRegistrar storageRegistrar) {
        val storages = storageRegistrar.getStorages();
        storages.forEach(storage -> {
            val entityClass = storage.entityClass();
            if (!entityClass.isAnnotationPresent(Entity.class)) {
                return;
            }
            sources.addAnnotatedClass(entityClass);
        });
        if (sources.getAnnotatedClasses().isEmpty()) {
            log.warn("No JPA @Entity classes were registered. Are your storage interfaces correctly annotated?");
        }
        return sources.buildMetadata().buildSessionFactory();
    }

    @Provide
    @Priority(4)
    @ActivateWhenBeanMissing(EntityManagerFactory.class)
    EntityManagerFactory entityManagerFactory(SessionFactory sessionFactory) {
        return sessionFactory.unwrap(EntityManagerFactory.class);
    }

    @Provide
    @Priority(5)
    @ActivateWhenBeanMissing(PlatformTransactionManager.class)
    PlatformTransactionManager transactionManager(EntityManagerFactory emf) {
        return new DefaultJPATransactionManager(emf);
    }

    @Provide
    @Priority(6)
    @ActivateWhenBeanMissing(TransactionalInterceptor.class)
    TransactionalInterceptor transactionInterceptor(PlatformTransactionManager transactionManager) {
        return new TransactionalInterceptor(transactionManager, pluginBeanFactory);
    }

    @Provide
    @Priority(7)
    @ActivateWhenBeanMissing(TransactionCleanupInterceptor.class)
    TransactionCleanupInterceptor transactionInterceptor() {
        return new TransactionCleanupInterceptor(pluginBeanFactory);
    }
}