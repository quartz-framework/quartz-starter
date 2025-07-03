package xyz.quartzframework.data.properties;

import lombok.Getter;
import xyz.quartzframework.core.bean.annotation.Injectable;
import xyz.quartzframework.core.property.Property;

@Getter
@Injectable
public class JPAPersistenceProperties {

    @Property("${quartz.data.jpa.datasource.url:jdbc:h2:mem:testdb}")
    private String datasourceUrl;

    @Property("${quartz.data.jpa.datasource.driver:}")
    private String datasourceDriver;

    @Property("${quartz.data.jpa.datasource.username:sa}")
    private String datasourceUsername;

    @Property("${quartz.data.jpa.datasource.password:}")
    private String datasourcePassword;

    @Property("${quartz.data.jpa.connection.pool-size:10}")
    private int connectionPoolSize;

    @Property("${quartz.data.jpa.connection.isolation:-1}")
    private int connectionIsolation;

    @Property("${quartz.data.jpa.connection.autocommit:true}")
    private boolean connectionAutocommit;

    @Property("${quartz.data.jpa.connection.provider-disables-autocommit:false}")
    private boolean providerDisablesAutocommit;

}