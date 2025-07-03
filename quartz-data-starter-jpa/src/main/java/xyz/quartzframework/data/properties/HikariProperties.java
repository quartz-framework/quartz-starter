package xyz.quartzframework.data.properties;

import lombok.Getter;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.property.Property;

@Getter
@Injectable
public class HikariProperties {

    @Property("${quartz.data.hikari.minimum-idle:5}")
    private int hikariMinimumIdle;

    @Property("${quartz.data.hikari.maximum-pool-size:10}")
    private int hikariMaximumPoolSize;

    @Property("${quartz.data.hikari.connection-timeout:30000}")
    private long hikariConnectionTimeout;

    @Property("${quartz.data.hikari.idle-timeout:600000}")
    private long hikariIdleTimeout;

    @Property("${quartz.data.hikari.max-lifetime:1800000}")
    private long hikariMaxLifetime;

    @Property("${quartz.data.hikari.keepalive-time:0}")
    private long hikariKeepaliveTime;

    @Property("${quartz.data.hikari.leak-detection-threshold:0}")
    private long hikariLeakDetectionThreshold;

    @Property("${quartz.data.hikari.validation-timeout:5000}")
    private long hikariValidationTimeout;

    @Property("${quartz.data.hikari.connection-test-query:}")
    private String hikariConnectionTestQuery;

    @Property("${quartz.data.hikari.connection-init-sql:}")
    private String hikariConnectionInitSql;

    @Property("${quartz.data.hikari.pool-name:QuartzHikariPool}")
    private String hikariPoolName;

    @Property("${quartz.data.hikari.auto-commit:true}")
    private boolean hikariAutoCommit;

    @Property("${quartz.data.hikari.read-only:false}")
    private boolean hikariReadOnly;

    @Property("${quartz.data.hikari.isolate-internal-queries:false}")
    private boolean hikariIsolateInternalQueries;

    @Property("${quartz.data.hikari.register-mbeans:false}")
    private boolean hikariRegisterMbeans;

    @Property("${quartz.data.hikari.allow-pool-suspension:false}")
    private boolean hikariAllowPoolSuspension;

    @Property("${quartz.data.hikari.initialization-fail-timeout:1}")
    private long hikariInitializationFailTimeout;

    @Property("${quartz.data.hikari.transaction-isolation:}")
    private String hikariTransactionIsolation;

    @Property("${quartz.data.hikari.schema:}")
    private String hikariSchema;
}