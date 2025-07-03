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
        }
        config.setJdbcUrl(jpaProps.getDatasourceUrl());
        config.setUsername(jpaProps.getDatasourceUsername());
        config.setPassword(jpaProps.getDatasourcePassword());
        config.setDriverClassName(driver);
        config.setMaximumPoolSize(jpaProps.getConnectionPoolSize());
        config.setAutoCommit(jpaProps.isConnectionAutocommit());

        config.setMinimumIdle(hikariProps.getHikariMinimumIdle());
        config.setMaximumPoolSize(hikariProps.getHikariMaximumPoolSize());
        config.setConnectionTimeout(hikariProps.getHikariConnectionTimeout());
        config.setIdleTimeout(hikariProps.getHikariIdleTimeout());
        config.setMaxLifetime(hikariProps.getHikariMaxLifetime());
        config.setKeepaliveTime(hikariProps.getHikariKeepaliveTime());
        config.setLeakDetectionThreshold(hikariProps.getHikariLeakDetectionThreshold());
        config.setValidationTimeout(hikariProps.getHikariValidationTimeout());
        config.setAutoCommit(hikariProps.isHikariAutoCommit());
        config.setReadOnly(hikariProps.isHikariReadOnly());
        config.setIsolateInternalQueries(hikariProps.isHikariIsolateInternalQueries());
        config.setRegisterMbeans(hikariProps.isHikariRegisterMbeans());
        config.setAllowPoolSuspension(hikariProps.isHikariAllowPoolSuspension());
        config.setInitializationFailTimeout(hikariProps.getHikariInitializationFailTimeout());
        config.setPoolName(hikariProps.getHikariPoolName());

        if (!hikariProps.getHikariConnectionTestQuery().isBlank())
            config.setConnectionTestQuery(hikariProps.getHikariConnectionTestQuery());

        if (!hikariProps.getHikariConnectionInitSql().isBlank())
            config.setConnectionInitSql(hikariProps.getHikariConnectionInitSql());

        if (!hikariProps.getHikariTransactionIsolation().isBlank())
            config.setTransactionIsolation(hikariProps.getHikariTransactionIsolation());

        if (!hikariProps.getHikariSchema().isBlank())
            config.setSchema(hikariProps.getHikariSchema());
        log.info("Configured HikariCP: url={}, user={}, driver={}", jpaProps.getDatasourceUrl(), jpaProps.getDatasourceUsername(), driver);
        return new HikariDataSource(config);
    }
}
