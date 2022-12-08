package org.sms.util;

import smartlib.database.Database;

import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DBUtil {
    public static int crud(PreparedStatement stmt, Object... arguments) throws SQLException {
        try {
            Object[] objs = Arrays.stream(arguments).toArray();
            for (int i = 0; i < objs.length; i++) {
                if (objs[i] instanceof Timestamp) {
                    stmt.setTimestamp(i + 1, (Timestamp) objs[i]);
                } else {
                    stmt.setString(i + 1, Objects.toString(objs[i], null));
                }
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static ResultSet query(PreparedStatement stmt, Object... arguments) throws SQLException {
        try {
            Object[] objs = Arrays.stream(arguments).toArray();
            for (int i = 0; i < objs.length; i++) {
                stmt.setString(i + 1, Objects.toString(objs[i], null));
            }
            return stmt.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static Map convertToMap(ResultSet rs) throws SQLException {
        try {
            Map mpReturn = new HashMap();
            int iCol = rs.getMetaData().getColumnCount();
            if (iCol >= 2) {
                while (rs.next()) {
                    Object objKey = rs.getObject(1);
                    Object objValue = rs.getObject(2);
                    mpReturn.put(objKey, objValue);
                }
                return mpReturn;
            } else {
                throw new SQLException("can't convert to a Map");
            }
        } finally {
            Database.closeObject(rs);
        }
    }

    public static Map queryToMap(Connection conn, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            Object[] objs = Arrays.stream(arguments).toArray();
            for (int i = 0; i < objs.length; i++) {
                stmt.setString(i + 1, Objects.toString(objs[i], null));
            }
            return convertToMap(stmt.executeQuery());
        }
    }

    public static long queryLong(PreparedStatement stmt, Object... arguments) throws SQLException {
        ResultSet rs = null;
        try {
            rs = query(stmt, arguments);
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Database.closeObject(rs);
        }
        return -1;
    }

    public static String queryString(PreparedStatement stmt, Object... arguments) throws SQLException {
        ResultSet rs = null;
        try {
            rs = query(stmt, arguments);
            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        } finally {
            Database.closeObject(rs);
        }
        return null;
    }

    public static void closeObject(PreparedStatement... statements) {
        Object[] stmts = Arrays.stream(statements).toArray();
        for (Object stmt : stmts) {
            Database.closeObject((Statement) stmt);
        }
    }

    public static void closeObject(ResultSet... resultSets) {
        Object[] stmts = Arrays.stream(resultSets).toArray();
        for (Object stmt : stmts) {
            Database.closeObject((ResultSet) stmt);
        }
    }
}
