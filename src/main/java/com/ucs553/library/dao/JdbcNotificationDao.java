package com.ucs553.library.dao;

import com.ucs553.library.model.Notification;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class JdbcNotificationDao extends AbstractJdbcDao<Notification> implements NotificationDao {

    public JdbcNotificationDao(ConnectionSource source) {
        super(source, "notifications");
    }

    @Override
    public List<Notification> findByMember(int memberId) throws DataAccessException {
        return query("SELECT * FROM notifications WHERE member_id = ? ORDER BY id DESC",
                ps -> ps.setInt(1, memberId), this::mapRow);
    }

    @Override
    public int countUnread(int memberId) throws DataAccessException {
        return count("SELECT COUNT(*) FROM notifications WHERE member_id = ? AND is_read = 0",
                ps -> ps.setInt(1, memberId));
    }

    @Override
    public void markAllRead(int memberId) throws DataAccessException {
        execute("UPDATE notifications SET is_read = 1 WHERE member_id = ?", ps -> ps.setInt(1, memberId));
    }

    @Override
    protected Notification mapRow(ResultSet rs) throws SQLException {
        return new Notification(rs.getInt("id"), rs.getInt("member_id"), rs.getString("message"),
                LocalDateTime.parse(rs.getString("created_at")), rs.getInt("is_read") == 1);
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO notifications (member_id, message, created_at, is_read) VALUES (?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE notifications SET member_id=?, message=?, created_at=?, is_read=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Notification n) throws SQLException {
        bindColumns(ps, n);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Notification n) throws SQLException {
        bindColumns(ps, n);
        ps.setInt(5, n.getId());
    }

    private void bindColumns(PreparedStatement ps, Notification n) throws SQLException {
        ps.setInt(1, n.getMemberId());
        ps.setString(2, n.getMessage());
        ps.setString(3, n.getCreatedAt().toString());
        ps.setInt(4, n.isRead() ? 1 : 0);
    }

    @Override
    protected int idOf(Notification n) {
        return n.getId();
    }

    @Override
    protected void assignId(Notification n, int id) {
        n.setId(id);
    }
}
