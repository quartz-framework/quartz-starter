package xyz.quartzframework.data.configurer;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.hibernate.cfg.Environment;
import org.hibernate.jpa.HibernatePersistenceProvider;
import xyz.quartzframework.core.QuartzPlugin;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.bean.factory.PluginBeanFactory;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.EnableTransactionalSupport;
import xyz.quartzframework.data.helper.AutoDialectHelper;
import xyz.quartzframework.data.properties.HibernateProperties;
import xyz.quartzframework.data.properties.JPAPersistenceProperties;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

@Slf4j
@Configurer(force = true)
@RequiredArgsConstructor
public class HibernateConfigurer {

    private final QuartzPlugin<?> quartzPlugin;

    private final PluginBeanFactory pluginBeanFactory;

    private final URLClassLoader classLoader;

    private final HibernateProperties hibernateProperties;

    private final JPAPersistenceProperties jpaProperties;

    @Provide
    HibernatePersistenceProvider hibernatePersistenceProvider() {
        return new HibernatePersistenceProvider();
    }

    @Provide
    PersistenceUnitInfo persistenceUnitInfo(DataSource dataSource) {
        val name = "%s-default".formatted(quartzPlugin.getName().toLowerCase());

        return new PersistenceUnitInfo() {

            @Override
            public String getPersistenceUnitName() {
                return name;
            }

            @Override
            public ClassLoader getClassLoader() {
                return classLoader;
            }

            @Override
            public void addTransformer(ClassTransformer classTransformer) {

            }

            @Override
            public ClassLoader getNewTempClassLoader() {
                return null;
            }

            @Override
            public Properties getProperties() {
                return new Properties();
            }

            @Override
            public String getPersistenceXMLSchemaVersion() {
                return "";
            }

            @Override
            public String getPersistenceProviderClassName() {
                return HibernatePersistenceProvider.class.getName();
            }

            @Override
            public PersistenceUnitTransactionType getTransactionType() {
                return isJTADisabled() ? PersistenceUnitTransactionType.RESOURCE_LOCAL : PersistenceUnitTransactionType.JTA;
            }

            @Override
            public DataSource getJtaDataSource() {
                return dataSource;
            }

            @Override
            public DataSource getNonJtaDataSource() {
                return dataSource;
            }

            @Override
            public List<String> getMappingFileNames() {
                return List.of();
            }

            @Override
            public List<URL> getJarFileUrls() {
                try {
                    return Collections.list(classLoader.getResources(""));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }

            @Override
            public URL getPersistenceUnitRootUrl() {
                return null;
            }

            @Override
            public List<String> getManagedClassNames() {
                return List.of();
            }

            @Override
            public boolean excludeUnlistedClasses() {
                return false;
            }

            @Override
            public SharedCacheMode getSharedCacheMode() {
                return null;
            }

            @Override
            public ValidationMode getValidationMode() {
                return null;
            }
        };
    }

    @Provide
    EntityManagerFactory entityManagerFactory(HibernatePersistenceProvider provider,
                                              PersistenceUnitInfo persistenceUnitInfo,
                                              DataSource dataSource) {
        return provider.createContainerEntityManagerFactory(persistenceUnitInfo, getHibernateSettings(dataSource));
    }

    private Map<String, Object> getHibernateSettings(DataSource dataSource) {
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
        } else {
            log.info("Using Hibernate dialect: {}", dialect);
        }
        settings.put(Environment.DIALECT, dialect);
        if (isJTADisabled()) {
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
        return settings;
    }

    private boolean isJTADisabled() {
        return pluginBeanFactory.getBeansWithAnnotation(EnableTransactionalSupport.class).isEmpty();
    }
}