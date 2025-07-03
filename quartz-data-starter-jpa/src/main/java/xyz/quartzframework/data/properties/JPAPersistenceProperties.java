package xyz.quartzframework.data.properties;

import lombok.Value;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.property.Property;

@Value
@Injectable
public class JPAPersistenceProperties {

    @Property("${quartz.data.jpa.datasource.url:jdbc:h2:mem:testdb}")
    String datasourceUrl;

    @Property("${quartz.data.jpa.datasource.driver:org.h2.Driver}")
    String datasourceDriver;

    @Property("${quartz.data.jpa.datasource.username:sa}")
    String datasourceUsername;

    @Property("${quartz.data.jpa.datasource.password:}")
    String datasourcePassword;

    @Property("${quartz.data.jpa.connection.pool-size:10}")
    int connectionPoolSize;

    @Property("${quartz.data.jpa.connection.isolation:-1}")
    int connectionIsolation;

    @Property("${quartz.data.jpa.connection.autocommit:true}")
    boolean connectionAutocommit;

    @Property("${quartz.data.jpa.connection.provider-disables-autocommit:false}")
    boolean providerDisablesAutocommit;

}