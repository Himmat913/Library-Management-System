package com.ucs553.library.dao;

import com.ucs553.library.model.AuditAction;
import com.ucs553.library.model.AuditEntry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcAuditDao extends AbstractJdbcDao<AuditEntry> implements AuditDao {

    public JdbcAuditDao(ConnectionSource source) {
        super(source, "audit_log");
    }

    @Override
    public List<AuditEntry> findRecent(int limit) throws DataAccessException {
        return query("SELECT * FROM audit_log ORDER BY id DESC LIMIT ?", ps -> ps.setInt(1, limit), this::mapRow);
    }

    @Override
    protected AuditEntry mapRow(ResultSet rs) throws SQLException {
        return new AuditEntry(rs.getInt("id"), LocalDateTime.parse(rs.getString("at")), rs.getString("actor"),
                AuditAction.valueOf(rs.getString("action")), rs.getString("details"));
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO audit_log (at, actor, action, details) VALUES (?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE audit_log SET at=?, actor=?, action=?, details=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, AuditEntry e) throws SQLException {
        bindColumns(ps, e);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, AuditEntry e) throws SQLException {
        bindColumns(ps, e);
        ps.setInt(5, e.getId());
    }

    private void bindColumns(PreparedStatement ps, AuditEntry e) throws SQLException {
        ps.setString(1, e.getAt().toString());
        ps.setString(2, e.getActor());
        ps.setString(3, e.getAction().name());
        ps.setString(4, e.getDetails());
    }

    @Override
    protected int idOf(AuditEntry e) {
        return e.getId();
    }

    @Override
    protected void assignId(AuditEntry e, int id) {
        e.setId(id);
    }
}
