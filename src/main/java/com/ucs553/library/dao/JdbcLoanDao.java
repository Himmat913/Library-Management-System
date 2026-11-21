package com.ucs553.library.dao;

import com.ucs553.library.model.Loan;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class JdbcLoanDao extends AbstractJdbcDao<Loan> implements LoanDao {

    public JdbcLoanDao(ConnectionSource source) {
        super(source, "loans");
    }

    @Override
    public List<Loan> findActive() throws DataAccessException {
        return query("SELECT * FROM loans WHERE returned_on IS NULL ORDER BY due_on", ps -> { }, this::mapRow);
    }

    @Override
    public List<Loan> findActiveByMember(int memberId) throws DataAccessException {
        return query("SELECT * FROM loans WHERE member_id = ? AND returned_on IS NULL ORDER BY due_on",
                ps -> ps.setInt(1, memberId), this::mapRow);
    }

    @Override
    public List<Loan> findOverdue(LocalDate today) throws DataAccessException {
        return query("SELECT * FROM loans WHERE returned_on IS NULL AND due_on < ? ORDER BY due_on",
                ps -> ps.setString(1, today.toString()), this::mapRow);
    }

    @Override
    public int countActiveByMember(int memberId) throws DataAccessException {
        return count("SELECT COUNT(*) FROM loans WHERE member_id = ? AND returned_on IS NULL",
                ps -> ps.setInt(1, memberId));
    }

    @Override
    protected Loan mapRow(ResultSet rs) throws SQLException {
        String returned = rs.getString("returned_on");
        Loan loan = new Loan(rs.getInt("id"), rs.getInt("item_id"), rs.getInt("member_id"),
                LocalDate.parse(rs.getString("issued_on")), LocalDate.parse(rs.getString("due_on")),
                returned == null ? null : LocalDate.parse(returned));
        loan.setRenewCount(rs.getInt("renew_count"));
        return loan;
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO loans (item_id, member_id, issued_on, due_on, returned_on, renew_count) VALUES (?,?,?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE loans SET item_id=?, member_id=?, issued_on=?, due_on=?, returned_on=?, renew_count=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Loan loan) throws SQLException {
        bindColumns(ps, loan);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Loan loan) throws SQLException {
        bindColumns(ps, loan);
        ps.setInt(7, loan.getId());
    }

    private void bindColumns(PreparedStatement ps, Loan loan) throws SQLException {
        ps.setInt(1, loan.getItemId());
        ps.setInt(2, loan.getMemberId());
        ps.setString(3, loan.getIssuedOn().toString());
        ps.setString(4, loan.getDueOn().toString());
        ps.setString(5, loan.getReturnedOn() == null ? null : loan.getReturnedOn().toString());
        ps.setInt(6, loan.getRenewCount());
    }

    @Override
    protected int idOf(Loan loan) {
        return loan.getId();
    }

    @Override
    protected void assignId(Loan loan, int id) {
        loan.setId(id);
    }
}
