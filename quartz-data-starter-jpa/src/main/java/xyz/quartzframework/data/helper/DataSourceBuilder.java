package xyz.quartzframework.data.helper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import xyz.quartzframework.data.properties.HikariProperties;
import xyz.quartzframework.data.properties.JPAPersistenceProperties;

import javax.sql.DataSource;
import java.net.URLClassLoader;

@Slf4j
public class DataSourceBuilder {

    public static DataSource build(JPAPersistenceProperties jpaProps, HikariProperties hikariProps, URLClassLoader classLoader) {
        if (jpaProps.getDatasourceUrl() == null || jpaProps.getDatasourceUrl().isBlank()) {
            throw new IllegalStateException("Datasource URL must be provided");
        }
        val config = new HikariConfig();
        String driver = jpaProps.getDatasourceDriver();
        if (driver == null || driver.isBlank()) {
            driver = AutoDialectHelper.resolveDriver(classLoader);
            if (driver == null) {
                throw new IllegalStateException("Could not determine JDBC driver: no known driver found on classpath.");
            }
            log.info("Auto-detected JDBC driver: {}", driver);
        } else {
            log.info("Using JDBC driver: {}", driver);
        }
        try {
            Class.forName(driver);
            log.debug("JDBC driver {} loaded", driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load JDBC driver: " + driver, e);
        }
        config.setJdbcUrl(jpaProps.getDatasourceUrl());
        config.setUsername(jpaProps.getDatasourceUsername());
        config.setPassword(jpaProps.getDatasourcePassword());
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(jpaProps.getConnectionPoolSize());
        config.setAutoCommit(jpaProps.isConnectionAutocommit());

        config.setMinimumIdle(hikariProps.getMinimumIdle());
        config.setMaximumPoolSize(hikariProps.getMaximumPoolSize());
        config.setConnectionTimeout(hikariProps.getConnectionTimeout());
        config.setIdleTimeout(hikariProps.getIdleTimeout());
        config.setMaxLifetime(hikariProps.getMaxLifetime());
        config.setKeepaliveTime(hikariProps.getKeepaliveTime());
        config.setLeakDetectionThreshold(hikariProps.getLeakDetectionThreshold());
        config.setValidationTimeout(hikariProps.getValidationTimeout());
        config.setAutoCommit(hikariProps.isAutoCommit());
        config.setReadOnly(hikariProps.isReadOnly());
        config.setIsolateInternalQueries(hikariProps.isHikariIsolateInternalQueries());
        config.setRegisterMbeans(hikariProps.isHikariRegisterMbeans());
        config.setAllowPoolSuspension(hikariProps.isHikariAllowPoolSuspension());
        config.setInitializationFailTimeout(hikariProps.getHikariInitializationFailTimeout());
        config.setPoolName(hikariProps.getPoolName());

        if (!hikariProps.getConnectionTestQuery().isBlank())
            config.setConnectionTestQuery(hikariProps.getConnectionTestQuery());

        if (!hikariProps.getConnectionInitSql().isBlank())
            config.setConnectionInitSql(hikariProps.getConnectionInitSql());

        if (!hikariProps.getHikariTransactionIsolation().isBlank())
            config.setTransactionIsolation(hikariProps.getHikariTransactionIsolation());

        if (!hikariProps.getHikariSchema().isBlank())
            config.setSchema(hikariProps.getHikariSchema());
        log.info("Configured HikariCP: url={}, user={}, driver={}", jpaProps.getDatasourceUrl(), jpaProps.getDatasourceUsername(), driver);
        return new HikariDataSource(config);
    }
}
