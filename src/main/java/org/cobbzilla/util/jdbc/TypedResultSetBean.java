package org.cobbzilla.util.jdbc;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.reflect.ReflectionUtil;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static org.cobbzilla.util.reflect.ReflectionUtil.getDeclaredField;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;
import static org.cobbzilla.util.string.StringUtil.snakeCaseToCamelCase;

@Slf4j
public class TypedResultSetBean<T> extends ResultSetBean implements Iterable<T> {

    public TypedResultSetBean(Class<T> clazz, ResultSet rs) throws SQLException { super(rs); rowType = clazz; }
    public TypedResultSetBean(Class<T> clazz, PreparedStatement ps) throws SQLException { super(ps); rowType = clazz; }
    public TypedResultSetBean(Class<T> clazz, Connection conn, String sql) throws SQLException { super(conn, sql); rowType = clazz; }

    private final Class<T> rowType;
    @Getter(lazy=true, value=AccessLevel.PRIVATE) private final List<T> typedRows = getTypedRows(rowType);

    @Override public Iterator<T> iterator() { return new ArrayList<>(getTypedRows()).iterator(); }

    private List<T> getTypedRows(Class<T> clazz) {
        final List<T> typedRows = new ArrayList<>();
        for (Map<String, Object> row : getRows()) {
            final T thing = instantiate(clazz);
            for (String name : row.keySet()) {
                final String field = snakeCaseToCamelCase(name);
                try {
                    final Object value = row.get(name);
                    readField(thing, field, value);
                } catch (Exception e) {
                    log.warn("getTypedRows: error setting "+field+": "+e);
                }
            }
            typedRows.add(thing);
        }
        return typedRows;
    }

    protected void readField(T thing, String field, Object value) {
        if (value != null) {
            try {
                ReflectionUtil.set(thing, field, value);
            } catch (Exception e) {
                // try field setter
                try {
                    final Field f = getDeclaredField(thing.getClass(), field);
                    if (f != null) {
                        f.setAccessible(true);
                        f.set(thing, value);
                    } else {
                        log.warn("readField: field "+thing.getClass().getName()+"."+field+" not found via setter nor via field: "+e);
                    }
                } catch (Exception e2) {
                    log.warn("readField: field "+thing.getClass().getName()+"."+field+" not found via setter nor via field: "+e2);
                }
            }
        }
    }

    public <K> Map<K, T> map (String field) {
        final Map<K, T> map = new HashMap<>();
        for (T thing : this) {
            map.put((K) ReflectionUtil.get(thing, field), thing);
        }
        return map;
    }

    public T firstObject() {
        final Iterator<T> iter = iterator();
        return iter.hasNext() ? iter.next() : null;
    }

}
