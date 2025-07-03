package xyz.quartzframework.data.properties;

import lombok.Value;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.property.Property;

@Value
@Injectable
public class HikariProperties {

    @Property("${quartz.data.hikari.minimum-idle:5}")
    int hikariMinimumIdle;

    @Property("${quartz.data.hikari.maximum-pool-size:10}")
    int hikariMaximumPoolSize;

    @Property("${quartz.data.hikari.connection-timeout:30000}")
    long hikariConnectionTimeout;

    @Property("${quartz.data.hikari.idle-timeout:600000}")
    long hikariIdleTimeout;

    @Property("${quartz.data.hikari.max-lifetime:1800000}")
    long hikariMaxLifetime;

    @Property("${quartz.data.hikari.keepalive-time:0}")
    long hikariKeepaliveTime;

    @Property("${quartz.data.hikari.leak-detection-threshold:0}")
    long hikariLeakDetectionThreshold;

    @Property("${quartz.data.hikari.validation-timeout:5000}")
    long hikariValidationTimeout;

    @Property("${quartz.data.hikari.connection-test-query:}")
    String hikariConnectionTestQuery;

    @Property("${quartz.data.hikari.connection-init-sql:}")
    String hikariConnectionInitSql;

    @Property("${quartz.data.hikari.pool-name:QuartzHikariPool}")
    String hikariPoolName;

    @Property("${quartz.data.hikari.auto-commit:true}")
    boolean hikariAutoCommit;

    @Property("${quartz.data.hikari.read-only:false}")
    boolean hikariReadOnly;

    @Property("${quartz.data.hikari.isolate-internal-queries:false}")
    boolean hikariIsolateInternalQueries;

    @Property("${quartz.data.hikari.register-mbeans:false}")
    boolean hikariRegisterMbeans;

    @Property("${quartz.data.hikari.allow-pool-suspension:false}")
    boolean hikariAllowPoolSuspension;

    @Property("${quartz.data.hikari.initialization-fail-timeout:1}")
    long hikariInitializationFailTimeout;

    @Property("${quartz.data.hikari.transaction-isolation:}")
    String hikariTransactionIsolation;

    @Property("${quartz.data.hikari.schema:}")
    String hikariSchema;
}