package xyz.quartzframework.data.configurer;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.core.io.ResourceLoader;
import xyz.quartzframework.core.bean.annotation.Preferred;
import xyz.quartzframework.core.bean.annotation.Provide;
import xyz.quartzframework.core.condition.annotation.ActivateWhenClassPresent;
import xyz.quartzframework.core.context.annotation.Configurer;
import xyz.quartzframework.data.properties.FlywayProperties;

import javax.sql.DataSource;

@Slf4j
@ActivateWhenClassPresent(classNames = "org.flywaydb.core.Flyway")
@Configurer(force = true)
public class FlywayConfigurer {

    @Provide
    @Preferred
    public Flyway flyway(ResourceLoader resourceLoader, DataSource dataSource, FlywayProperties properties) {
        log.info("Enabling Flyway support...");
        Flyway flyway = Flyway.configure(resourceLoader.getClassLoader())
                .dataSource(dataSource)
                .locations(properties.getLocations())
                .table(properties.getTable())
                .schemas(properties.getSchemas())
                .baselineDescription(properties.getBaselineDescription() == null ? "" : properties.getBaselineDescription())
                .baselineOnMigrate(properties.isBaselineOnMigrate())
                .baselineVersion(properties.getBaselineVersion())
                .cleanDisabled(properties.isCleanDisabled())
                .validateOnMigrate(properties.isValidateOnMigrate())
                .outOfOrder(properties.isOutOfOrder())
                .load();
        if (properties.isEnabled()) {
            flyway.migrate();
        }
        return flyway;
    }
}