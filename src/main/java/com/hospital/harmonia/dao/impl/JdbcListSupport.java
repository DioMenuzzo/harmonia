package com.hospital.harmonia.dao.impl;

import com.hospital.harmonia.config.DatabaseConfig;
import com.hospital.harmonia.dao.DataAccessException;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared "run this SELECT, map each row, collect into a List" pattern --
 * previously duplicated (byte-for-byte except for the mapped type, the
 * mapper method and the log/error messages) as EmployeeDaoImpl.list(String)
 * and EquipmentDaoImpl.list(String); see the audit's "3.5 Duplicação de
 * código".
 *
 * MealDaoImpl.runQuery is deliberately NOT folded in here: it takes an
 * already-parameterized PreparedStatement (not a raw SQL string) and
 * inlines its own row mapping instead of delegating to a separate map()
 * method, so unifying it would mean either a second near-duplicate
 * overload or changing its behavior -- not worth it just to remove a name
 * difference.
 *
 * Package-private: every caller lives in this same dao.impl package.
 */
final class JdbcListSupport {

    private JdbcListSupport() {
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /**
     * Runs {@code sql} as a plain (non-parameterized) query and maps every
     * row with {@code mapper}. The caller's own Logger is used (not one
     * scoped to this class) so the log entry still shows which DAO actually
     * ran the query, and {@code logMessage}/{@code userMessage} let each
     * caller keep its own wording for the log line and for the
     * DataAccessException shown further up.
     */
    static <T> List<T> queryList(Logger log, String sql, RowMapper<T> mapper,
                                  String logMessage, String userMessage) {
        List<T> result = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(mapper.map(rs));
            }
            return result;
        } catch (SQLException e) {
            log.error(logMessage, e);
            throw new DataAccessException(userMessage, e);
        }
    }
}
