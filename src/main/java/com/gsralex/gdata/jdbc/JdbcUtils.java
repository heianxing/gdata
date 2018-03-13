package com.gsralex.gdata.jdbc;


import com.gsralex.gdata.*;
import org.apache.log4j.Logger;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author gsralex
 *         2018/3/10
 */
public class JdbcUtils {

    private static Logger LOGGER = Logger.getLogger(JdbcUtils.class);

    private DataSource dataSource;
    private SqlCuHelper cudHelper;
    private SqlRHelper rHelper;


    public JdbcUtils(DataSource dataSource) {
        this.dataSource = dataSource;
        this.cudHelper = new SqlCuHelper();
        this.rHelper = new SqlRHelper();
    }

    public <T> boolean insert(T t) {
        return insert(t, false);
    }

    public <T> boolean insert(T t, boolean generatedKey) {
        if (t == null) {
            return false;
        }
        if (generatedKey) {
            return insertGeneratedKey(t);
        } else {
            return insertBean(t);
        }
    }

    private <T> boolean insertBean(T t) {
        if (t == null) {
            return false;
        }
        Object[] objects = cudHelper.getInsertObjects(t);
        String sql = cudHelper.getInsertSql(t.getClass());
        return executeUpdate(sql, objects) != 0 ? true : false;
    }

    private <T> boolean insertGeneratedKey(T t) {
        Object[] objects = cudHelper.getInsertObjects(t);
        String sql = cudHelper.getInsertSql(t.getClass());
        Object key = executeUpdateGenerateKey(sql, objects);
        if (key != null) {
            List<FieldColumn> columnList = cudHelper.getColumns(t.getClass(), FieldEnum.Id);
            if (columnList.size() != 0) {
                ModelMap modelMap = ModelMapper.getMapperCache(t.getClass());
                FieldValue fieldValue = new FieldValue(t, modelMap);
                for (FieldColumn column : columnList) {
                    fieldValue.setValue(column.getName(), key);
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public <T> int batchInsert(List<T> list) {
        if (list == null || list.size() == 0)
            return 0;
        Class<T> type = (Class<T>) list.get(0).getClass();
        String sql = cudHelper.getInsertSql(type);
        List<Object[]> objectList = new ArrayList<>();
        for (T t : list) {
            objectList.add(cudHelper.getInsertObjects(t));
        }
        return executeBatch(sql, objectList);
    }


    public <T> boolean update(T t) {
        if (t == null)
            return false;
        String sql = cudHelper.getUpdateSql(t.getClass());
        Object[] objects = cudHelper.getUpdateObjects(t);
        return executeUpdate(sql, objects) != 0 ? true : false;
    }

    public <T> int batchUpdate(List<T> list) {
        if (list == null || list.size() == 0) {
            return 0;
        }
        Class<T> type = (Class<T>) list.get(0).getClass();
        String sql = cudHelper.getUpdateSql(type);
        List<Object[]> objectList = new ArrayList<>();
        for (T t : list) {
            objectList.add(cudHelper.getUpdateObjects(t));
        }
        return executeBatch(sql, objectList);
    }

    public Object executeUpdateGenerateKey(String sql, Object[] objects) {
//        GeneratedKey key = new GeneratedKey();
        PreparedStatement ps = null;
        try {
            ps = pre(sql, objects, true);
            int r = ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (r != 0) {
                if (rs.next()) {
                    if (columnCount != 0) {
                        return rs.getObject(1);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.executeUpdate", e);

        } finally {
            close(ps);
        }
        return null;
    }


    public int executeUpdate(String sql, Object[] objects) {
        PreparedStatement ps = null;
        try {
            ps = pre(sql, objects);
            return ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.executeUpdate", e);
            return 0;
        } finally {
            close(ps);
        }
    }

    public <T> T get(String sql, Object[] objects, Class<T> type) {
        List<T> list = query(sql, objects, type);
        if (list != null && list.size() != 0) {
            return list.get(0);
        }
        return null;
    }

    public <T> List<T> query(String sql, Object[] objects, Class<T> type) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = pre(sql, objects);
            rs = ps.executeQuery();
            return mapperList(rs, type);
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.query", e);
            return null;
        } finally {
            closeRs(rs);
            close(ps);
        }
    }

    public int executeBatch(String sql, List<Object[]> objectList) {
        PreparedStatement ps = null;
        try {
            ps = preBatch(sql, objectList);
            int[] r = ps.executeBatch();
            ps.getConnection().commit();
            return getBatchResult(r);
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.executeBatch", e);
            return 0;
        } finally {
            close(ps);
        }
    }

    private int getBatchResult(int[] r) {
        int cnt = 0;
        for (int item : r) {
            if (item != Statement.EXECUTE_FAILED) {
                cnt++;
            }
        }
        return cnt;
    }

    private <T> List<T> mapperList(ResultSet rs, Class<T> type) {
        List<T> list = new ArrayList<>();
        try {
            while (rs.next()) {
                list.add(rHelper.mapperEntity(rs, type));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    private PreparedStatement pre(String sql, Object[] objects) {
        return pre(sql, objects, false);
    }

    private PreparedStatement pre(String sql, Object[] objects, boolean autoGeneratedKeys) {
        try {
            Connection conn = getConnection();
            PreparedStatement ps;
            if (autoGeneratedKeys) {
                ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            } else {
                ps = conn.prepareStatement(sql);
            }
            if (objects != null && objects.length != 0) {
                for (int i = 0; i < objects.length; i++) {
                    ps.setObject(i + 1, objects[i]);
                }
            }
            return ps;
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.pre", e);
            return null;
        }
    }

    private PreparedStatement preBatch(String sql, List<Object[]> objectsArray) {
        try {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            PreparedStatement ps = conn.prepareStatement(sql, ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            for (Object[] objects : objectsArray) {
                for (int i = 0, size = objects.length; i < size; i++) {
                    ps.setObject(i + 1, objects[i]);
                }
                ps.addBatch();
            }
            return ps;
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.preBatch", e);
            return null;
        }
    }

    private Connection getConnection() {
        try {
            return this.dataSource.getConnection();
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.getConnection", e);
            return null;
        }
    }

    private void close(PreparedStatement ps) {

        try {
            Connection conn = ps.getConnection();
            closePs(ps);
            closeConn(conn);
        } catch (SQLException e) {
            LOGGER.error("JdbcUtils.close", e);
        }
    }

    private void closePs(PreparedStatement ps) {
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                LOGGER.error("JdbcUtils.closePs", e);
            }
        }
    }

    private void closeRs(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                LOGGER.error("JdbcUtils.closeRs", e);
            }
        }
    }

    private void closeConn(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                LOGGER.error("JdbcUtils.closeConn", e);
            }
        }
    }
}
