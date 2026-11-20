package com.ucs553.library.dao;

import com.ucs553.library.model.Role;
import com.ucs553.library.model.Staff;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class JdbcStaffDao extends AbstractJdbcDao<Staff> implements StaffDao {

    public JdbcStaffDao(ConnectionSource source) {
        super(source, "staff");
    }

    @Override
    public Optional<Staff> findByUsername(String username) throws DataAccessException {
        return query("SELECT * FROM staff WHERE username = ?", ps -> ps.setString(1, username), this::mapRow)
                .stream().findFirst();
    }

    @Override
    public int countAdmins() throws DataAccessException {
        return count("SELECT COUNT(*) FROM staff WHERE role = 'ADMIN'", ps -> { });
    }

    @Override
    protected Staff mapRow(ResultSet rs) throws SQLException {
        Staff staff = new Staff(rs.getString("name"), rs.getString("email"), rs.getString("username"),
                Role.valueOf(rs.getString("role")), rs.getString("password_hash"));
        staff.setId(rs.getInt("id"));
        return staff;
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO staff (name, email, username, password_hash, role) VALUES (?,?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE staff SET name=?, email=?, username=?, password_hash=?, role=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Staff staff) throws SQLException {
        bindColumns(ps, staff);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Staff staff) throws SQLException {
        bindColumns(ps, staff);
        ps.setInt(6, staff.getId());
    }

    private void bindColumns(PreparedStatement ps, Staff staff) throws SQLException {
        ps.setString(1, staff.getName());
        ps.setString(2, staff.getEmail());
        ps.setString(3, staff.getUsername());
        ps.setString(4, staff.getPasswordHash());
        ps.setString(5, staff.getRole().name());
    }

    @Override
    protected int idOf(Staff staff) {
        return staff.getId();
    }

    @Override
    protected void assignId(Staff staff, int id) {
        staff.setId(id);
    }
}
