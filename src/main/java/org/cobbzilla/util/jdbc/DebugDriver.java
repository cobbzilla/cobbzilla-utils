package org.cobbzilla.util.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public interface DebugDriver {

    public Connection connect(String url, Properties info) throws SQLException;

}
