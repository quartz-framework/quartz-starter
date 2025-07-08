package xyz.quartzframework.data.configurer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenBeanMissing;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.helper.DataSourceBuilder;
import xyz.quartzframework.data.properties.HikariProperties;
import xyz.quartzframework.data.properties.JPAPersistenceProperties;

import javax.sql.DataSource;
import java.net.URLClassLoader;

@Slf4j
@Configurer(force = true)
@RequiredArgsConstructor
public class DataSourceConfigurer {

    private final URLClassLoader classLoader;

    private final HikariProperties hikariProperties;

    private final JPAPersistenceProperties jpaProperties;

    @Provide
    @ActivateWhenBeanMissing(DataSource.class)
    public DataSource dataSource() {
        return DataSourceBuilder.build(jpaProperties, hikariProperties, classLoader);
    }
}