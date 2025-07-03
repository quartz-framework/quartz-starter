package xyz.quartzframework.data.helper;

import lombok.experimental.UtilityClass;
import org.hibernate.dialect.*;

import java.net.URLClassLoader;
import java.util.LinkedHashMap;
import java.util.Map;

@UtilityClass
public class AutoDialectHelper {

    private static final Map<String, String> DRIVER_MAP = new LinkedHashMap<>();
    private static final Map<String, String> DIALECT_MAP = new LinkedHashMap<>();

    static {
        DRIVER_MAP.put("org.h2.Driver", "org.h2.Driver");
        DIALECT_MAP.put("org.h2.Driver", H2Dialect.class.getName());

        DRIVER_MAP.put("org.postgresql.Driver", "org.postgresql.Driver");
        DIALECT_MAP.put("org.postgresql.Driver", PostgreSQLDialect.class.getName());

        DRIVER_MAP.put("com.mysql.cj.jdbc.Driver", "com.mysql.cj.jdbc.Driver");
        DIALECT_MAP.put("com.mysql.cj.jdbc.Driver", MySQLDialect.class.getName());

        DRIVER_MAP.put("oracle.jdbc.OracleDriver", "oracle.jdbc.OracleDriver");
        DIALECT_MAP.put("oracle.jdbc.OracleDriver", OracleDialect.class.getName());

        DRIVER_MAP.put("com.microsoft.sqlserver.jdbc.SQLServerDriver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DIALECT_MAP.put("com.microsoft.sqlserver.jdbc.SQLServerDriver", SQLServerDialect.class.getName());
    }

    public String resolveDriver(URLClassLoader classLoader) {
        for (String driverClass : DRIVER_MAP.keySet()) {
            if (isPresent(driverClass, classLoader)) {
                return DRIVER_MAP.get(driverClass);
            }
        }
        throw new IllegalStateException("Could not auto-detect JDBC driver. Please set 'quartz.jpa.datasource.driver' manually.");
    }

    public String resolveDialect(URLClassLoader classLoader) {
        for (Map.Entry<String, String> entry : DIALECT_MAP.entrySet()) {
            if (isPresent(entry.getKey(), classLoader)) {
                return entry.getValue();
            }
        }
        throw new IllegalStateException("Could not auto-detect Hibernate dialect. Please set 'quartz.jpa.dialect' manually.");
    }

    private boolean isPresent(String className, URLClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}