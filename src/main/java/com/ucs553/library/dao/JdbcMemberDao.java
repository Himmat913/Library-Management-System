package com.ucs553.library.dao;

import com.ucs553.library.model.Member;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JdbcMemberDao extends AbstractJdbcDao<Member> implements MemberDao {

    public JdbcMemberDao(ConnectionSource source) {
        super(source, "members");
    }

    @Override
    public Optional<Member> findByEmail(String email) throws DataAccessException {
        return query("SELECT * FROM members WHERE email = ?",
                ps -> ps.setString(1, email.trim().toLowerCase()), this::mapRow).stream().findFirst();
    }

    @Override
    public List<Member> search(String text) throws DataAccessException {
        String like = "%" + text.trim().toLowerCase() + "%";
        return query("SELECT * FROM members WHERE LOWER(name) LIKE ? OR email LIKE ? ORDER BY name",
                ps -> {
                    ps.setString(1, like);
                    ps.setString(2, like);
                }, this::mapRow);
    }

    @Override
    protected Member mapRow(ResultSet rs) throws SQLException {
        Member m = new Member(rs.getString("name"), rs.getString("email"),
                LocalDate.parse(rs.getString("joined_on")), rs.getInt("borrow_limit"));
        m.setId(rs.getInt("id"));
        return m;
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO members (name, email, joined_on, borrow_limit) VALUES (?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE members SET name=?, email=?, joined_on=?, borrow_limit=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Member m) throws SQLException {
        bindColumns(ps, m);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Member m) throws SQLException {
        bindColumns(ps, m);
        ps.setInt(5, m.getId());
    }

    private void bindColumns(PreparedStatement ps, Member m) throws SQLException {
        ps.setString(1, m.getName());
        ps.setString(2, m.getEmail());
        ps.setString(3, m.getJoinedOn().toString());
        ps.setInt(4, m.getBorrowLimit());
    }

    @Override
    protected int idOf(Member m) {
        return m.getId();
    }

    @Override
    protected void assignId(Member m, int id) {
        m.setId(id);
    }
}
