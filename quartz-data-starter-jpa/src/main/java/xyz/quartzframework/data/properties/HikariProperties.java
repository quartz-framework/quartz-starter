package xyz.quartzframework.data.properties;

import lombok.Getter;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.property.Property;

@Getter
@Injectable
public class HikariProperties {

    @Property("${quartz.data.hikari.minimum-idle:1}")
    private int minimumIdle;

    @Property("${quartz.data.hikari.maximum-pool-size:10}")
    private int maximumPoolSize;

    @Property("${quartz.data.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Property("${quartz.data.hikari.idle-timeout:600000}")
    private long idleTimeout;

    @Property("${quartz.data.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Property("${quartz.data.hikari.keepalive-time:0}")
    private long keepaliveTime;

    @Property("${quartz.data.hikari.leak-detection-threshold:0}")
    private long leakDetectionThreshold;

    @Property("${quartz.data.hikari.validation-timeout:5000}")
    private long validationTimeout;

    @Property("${quartz.data.hikari.connection-test-query:}")
    private String connectionTestQuery;

    @Property("${quartz.data.hikari.connection-init-sql:}")
    private String connectionInitSql;

    @Property("${quartz.data.hikari.pool-name:QuartzHikariPool}")
    private String poolName;

    @Property("${quartz.data.hikari.auto-commit:true}")
    private boolean autoCommit;

    @Property("${quartz.data.hikari.read-only:false}")
    private boolean readOnly;

    @Property("${quartz.data.hikari.isolate-internal-queries:false}")
    private boolean hikariIsolateInternalQueries;

    @Property("${quartz.data.hikari.register-mbeans:false}")
    private boolean hikariRegisterMbeans;

    @Property("${quartz.data.hikari.allow-pool-suspension:false}")
    private boolean hikariAllowPoolSuspension;

    @Property("${quartz.data.hikari.initialization-fail-timeout:-1}")
    private long hikariInitializationFailTimeout;

    @Property("${quartz.data.hikari.transaction-isolation:}")
    private String hikariTransactionIsolation;

    @Property("${quartz.data.hikari.schema:}")
    private String hikariSchema;
}