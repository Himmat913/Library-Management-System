package com.ucs553.library.dao;

import com.ucs553.library.model.Fine;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JdbcFineDao extends AbstractJdbcDao<Fine> implements FineDao {

    public JdbcFineDao(ConnectionSource source) {
        super(source, "fines");
    }

    @Override
    public List<Fine> findByMember(int memberId) throws DataAccessException {
        return query("SELECT * FROM fines WHERE member_id = ? ORDER BY id DESC",
                ps -> ps.setInt(1, memberId), this::mapRow);
    }

    @Override
    public List<Fine> findUnpaid() throws DataAccessException {
        return query("SELECT * FROM fines WHERE paid_on IS NULL ORDER BY id", ps -> { }, this::mapRow);
    }

    @Override
    protected Fine mapRow(ResultSet rs) throws SQLException {
        String paid = rs.getString("paid_on");
        return new Fine(rs.getInt("id"), rs.getInt("loan_id"), rs.getInt("member_id"),
                new BigDecimal(rs.getString("amount")), LocalDate.parse(rs.getString("created_on")),
                paid == null ? null : LocalDate.parse(paid));
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO fines (loan_id, member_id, amount, created_on, paid_on) VALUES (?,?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE fines SET loan_id=?, member_id=?, amount=?, created_on=?, paid_on=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Fine f) throws SQLException {
        bindColumns(ps, f);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Fine f) throws SQLException {
        bindColumns(ps, f);
        ps.setInt(6, f.getId());
    }

    private void bindColumns(PreparedStatement ps, Fine f) throws SQLException {
        ps.setInt(1, f.getLoanId());
        ps.setInt(2, f.getMemberId());
        ps.setString(3, f.getAmount().toPlainString());
        ps.setString(4, f.getCreatedOn().toString());
        ps.setString(5, f.getPaidOn() == null ? null : f.getPaidOn().toString());
    }

    @Override
    protected int idOf(Fine f) {
        return f.getId();
    }

    @Override
    protected void assignId(Fine f, int id) {
        f.setId(id);
    }
}
