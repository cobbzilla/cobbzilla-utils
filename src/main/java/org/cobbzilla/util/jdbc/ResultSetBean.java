package org.cobbzilla.util.jdbc;

import lombok.AccessLevel;
import lombok.Cleanup;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@NoArgsConstructor(access=AccessLevel.PRIVATE) @Slf4j
public class ResultSetBean {

    public static final ResultSetBean EMPTY = new ResultSetBean();

    @Getter private final ArrayList<Map<String, Object>> rows = new ArrayList<>();
    public boolean isEmpty () { return rows.isEmpty(); }

    public int rowCount () { return isEmpty() ? 0 : rows.size(); }

    public Map<String, Object> first () { return rows.get(0); }
    public Integer count () { return isEmpty() ? null : Integer.valueOf(rows.get(0).entrySet().iterator().next().getValue().toString()); }
    public int countOrZero () { return isEmpty() ? 0 : Integer.parseInt(rows.get(0).entrySet().iterator().next().getValue().toString()); }

    public ResultSetBean (ResultSet rs)                throws SQLException { rows.addAll(read(rs)); }
    public ResultSetBean (PreparedStatement ps)        throws SQLException { rows.addAll(read(ps)); }
    public ResultSetBean (Connection conn, String sql) throws SQLException { rows.addAll(read(conn, sql)); }

    private final AtomicReference<ResultSetMetaData> rsMetaData = new AtomicReference<>();
    public ResultSetMetaData getRsMetaData(ResultSet rs) throws SQLException {
        if (rsMetaData.get() == null) {
            synchronized (rsMetaData) {
                if (rsMetaData.get() == null) {
                    rsMetaData.set(rs.getMetaData());
                }
            }
        }
        return rsMetaData.get();
    }
    public ResultSetMetaData getRsMetaData() { return rsMetaData.get(); }

    private List<Map<String, Object>> read(Connection conn, String sql) throws SQLException {
        @Cleanup final PreparedStatement ps = conn.prepareStatement(sql);
        return read(ps);
    }
    private List<Map<String, Object>> read(PreparedStatement ps) throws SQLException {
        @Cleanup final ResultSet rs = ps.executeQuery();
        return read(rs);
    }

    private List<Map<String, Object>> read(ResultSet rs) throws SQLException {
        final ResultSetMetaData rsMetaData = getRsMetaData(rs);
        final int numColumns = rsMetaData.getColumnCount();
        final List<Map<String, Object>> results = new ArrayList<>();
        while (rs.next()){
            final HashMap<String, Object> row = row2map(rs, rsMetaData, numColumns);
            results.add(row);
        }
        return results;
    }

    public <T> List<T> getColumnValues (String column) {
        final List<T> values = new ArrayList<>();
        for (Map<String, Object> row : getRows()) values.add((T) row.get(column));
        return values;
    }

    public static HashMap<String, Object> row2map(ResultSet rs) throws SQLException {
        final ResultSetMetaData rsMetaData = rs.getMetaData();
        final int numColumns = rsMetaData.getColumnCount();
        return row2map(rs, rsMetaData, numColumns);
    }

    public static HashMap<String, Object> row2map(ResultSet rs, ResultSetMetaData rsMetaData) throws SQLException {
        final int numColumns = rsMetaData.getColumnCount();
        return row2map(rs, rsMetaData, numColumns);
    }

    public static HashMap<String, Object> row2map(ResultSet rs, ResultSetMetaData rsMetaData, int numColumns) throws SQLException {
        final HashMap<String, Object> row = new HashMap<>(numColumns);
        for(int i=1; i<=numColumns; ++i){
            row.put(rsMetaData.getColumnName(i), rs.getObject(i));
        }
        return row;
    }

    public static List<String> getColumns(ResultSetMetaData rsMetaData) throws SQLException {
        int columnCount = rsMetaData.getColumnCount();
        final List<String> columns = new ArrayList<>(columnCount);
        for (int i=1; i<=columnCount; ++i) {
            columns.add(rsMetaData.getColumnName(i));
        }
        return columns;
    }

}
