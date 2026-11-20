package com.ucs553.library.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractJdbcDao<T> implements Dao<T> {

    @FunctionalInterface
    public interface Binder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface RowMapper<R> {
        R map(ResultSet rs) throws SQLException;
    }

    private final ConnectionSource source;
    private final String table;

    protected AbstractJdbcDao(ConnectionSource source, String table) {
        this.source = source;
        this.table = table;
    }

    protected abstract T mapRow(ResultSet rs) throws SQLException;

    protected abstract String insertSql();

    protected abstract void bindInsert(PreparedStatement ps, T entity) throws SQLException;

    protected abstract String updateSql();

    // the id is bound last, as the final parameter of the update statement
    protected abstract void bindUpdate(PreparedStatement ps, T entity) throws SQLException;

    protected abstract int idOf(T entity);

    protected abstract void assignId(T entity, int id);

    @Override
    public int insert(T entity) throws DataAccessException {
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement(insertSql(), Statement.RETURN_GENERATED_KEYS)) {
            bindInsert(ps, entity);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    assignId(entity, id);
                    return id;
                }
            }
            throw new DataAccessException("Insert into " + table + " did not return an id");
        } catch (SQLException e) {
            throw new DataAccessException("Could not insert into " + table, e);
        }
    }

    @Override
    public void update(T entity) throws DataAccessException {
        int rows = execute(updateSql(), ps -> bindUpdate(ps, entity));
        if (rows == 0) {
            throw new DataAccessException("No row with id " + idOf(entity) + " in " + table);
        }
    }

    @Override
    public void delete(int id) throws DataAccessException {
        int rows = execute("DELETE FROM " + table + " WHERE id = ?", ps -> ps.setInt(1, id));
        if (rows == 0) {
            throw new DataAccessException("No row with id " + id + " in " + table);
        }
    }

    @Override
    public Optional<T> findById(int id) throws DataAccessException {
        return query("SELECT * FROM " + table + " WHERE id = ?", ps -> ps.setInt(1, id), this::mapRow)
                .stream().findFirst();
    }

    @Override
    public List<T> findAll() throws DataAccessException {
        return query("SELECT * FROM " + table + " ORDER BY id", ps -> { }, this::mapRow);
    }

    protected <R> List<R> query(String sql, Binder binder, RowMapper<R> mapper) throws DataAccessException {
        try (Connection c = source.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<R> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapper.map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Query on " + table + " failed", e);
        }
    }

    protected int count(String sql, Binder binder) throws DataAccessException {
        return query(sql, binder, rs -> rs.getInt(1)).get(0);
    }

    private int execute(String sql, Binder binder) throws DataAccessException {
        try (Connection c = source.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not change " + table, e);
        }
    }
}
