package org.cobbzilla.util.jdbc;

import lombok.Delegate;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public class DebugPostgresqlDriver implements Driver, DebugDriver {

    private static final String DEBUG_PREFIX = "debug:";
    private static final String POSTGRESQL_PREFIX = "jdbc:postgresql:";
    private static final String DRIVER_CLASS_NAME = "org.postgresql.Driver";

    static {
        try {
            java.sql.DriverManager.registerDriver(new DebugPostgresqlDriver());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Delegate(excludes = DebugDriver.class)
    private Driver driver = instantiate(DRIVER_CLASS_NAME);

    @Override public Connection connect(String url, Properties info) throws SQLException {
        if (url.startsWith(DEBUG_PREFIX)) {
            url = url.substring(DEBUG_PREFIX.length());
            if (url.startsWith(POSTGRESQL_PREFIX)) {
                return new DebugConnection(driver.connect(url, info));
            }
        }
        throw new IllegalArgumentException("can't connect: "+url);
    }
}
