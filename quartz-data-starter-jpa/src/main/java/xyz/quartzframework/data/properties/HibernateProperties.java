package xyz.quartzframework.data.properties;

import lombok.Value;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.property.Property;

@Value
@Injectable
public class HibernateProperties {

    @Property("${quartz.data.hibernate.dialect:org.hibernate.dialect.H2Dialect}")
    String dialect;

    @Property("${quartz.data.hibernate.ddl-auto:none}")
    String ddlAuto;

    @Property("${quartz.data.hibernate.show-sql:true}")
    boolean showSql;

    @Property("${quartz.data.hibernate.format-sql:false}")
    boolean formatSql;

    @Property("${quartz.data.hibernate.highlight-sql:false}")
    boolean highlightSql;

    @Property("${quartz.data.hibernate.log-slow-query:false}")
    boolean logSlowQuery;

    @Property("${quartz.data.hibernate.use-sql-comments:false}")
    boolean useSqlComments;

    @Property("${quartz.data.hibernate.fetch-size:-1}")
    int fetchSize;

    @Property("${quartz.data.hibernate.use-scrollable-resultset:true}")
    boolean useScrollableResultSet;

    @Property("${quartz.data.hibernate.lob.non-contextual-creation:true}")
    boolean nonContextualLobCreation;

    @Property("${quartz.data.hibernate.log-jdbc-warnings:true}")
    boolean logJdbcWarnings;

    @Property("${quartz.data.hibernate.jdbc.time-zone:}")
    String jdbcTimeZone;

    @Property("${quartz.data.hibernate.use-get-generated-keys:true}")
    boolean useGetGeneratedKeys;

    @Property("${quartz.data.hibernate.connection-handling:}")
    String connectionHandling;

    @Property("${quartz.data.hibernate.statement-inspector:}")
    String statementInspector;

    @Property("${quartz.data.hibernate.dialect.native-param-markers:true}")
    boolean dialectNativeParamMarkers;

}