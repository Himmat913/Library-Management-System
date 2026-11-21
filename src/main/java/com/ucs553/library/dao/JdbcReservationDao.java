package com.ucs553.library.dao;

import com.ucs553.library.model.Reservation;
import com.ucs553.library.model.ReservationStatus;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class JdbcReservationDao extends AbstractJdbcDao<Reservation> implements ReservationDao {

    public JdbcReservationDao(ConnectionSource source) {
        super(source, "reservations");
    }

    @Override
    public List<Reservation> findWaitingByItem(int itemId) throws DataAccessException {
        return query("SELECT * FROM reservations WHERE item_id = ? AND status = 'WAITING' ORDER BY id",
                ps -> ps.setInt(1, itemId), this::mapRow);
    }

    @Override
    public List<Reservation> findOpen() throws DataAccessException {
        return query("SELECT * FROM reservations WHERE status IN ('WAITING', 'READY') ORDER BY item_id, id",
                ps -> { }, this::mapRow);
    }

    @Override
    public List<Reservation> findByMember(int memberId) throws DataAccessException {
        return query("SELECT * FROM reservations WHERE member_id = ? ORDER BY id DESC",
                ps -> ps.setInt(1, memberId), this::mapRow);
    }

    @Override
    public Optional<Reservation> findOpenFor(int itemId, int memberId) throws DataAccessException {
        return query("SELECT * FROM reservations WHERE item_id = ? AND member_id = ? "
                        + "AND status IN ('WAITING', 'READY') ORDER BY id",
                ps -> {
                    ps.setInt(1, itemId);
                    ps.setInt(2, memberId);
                }, this::mapRow).stream().findFirst();
    }

    @Override
    public List<Reservation> findLapsedHolds(LocalDate today) throws DataAccessException {
        return query("SELECT * FROM reservations WHERE status = 'READY' AND hold_until < ? ORDER BY id",
                ps -> ps.setString(1, today.toString()), this::mapRow);
    }

    @Override
    protected Reservation mapRow(ResultSet rs) throws SQLException {
        String hold = rs.getString("hold_until");
        return new Reservation(rs.getInt("id"), rs.getInt("item_id"), rs.getInt("member_id"),
                LocalDate.parse(rs.getString("reserved_on")), ReservationStatus.valueOf(rs.getString("status")),
                hold == null ? null : LocalDate.parse(hold));
    }

    @Override
    protected String insertSql() {
        return "INSERT INTO reservations (item_id, member_id, reserved_on, status, hold_until) VALUES (?,?,?,?,?)";
    }

    @Override
    protected String updateSql() {
        return "UPDATE reservations SET item_id=?, member_id=?, reserved_on=?, status=?, hold_until=? WHERE id=?";
    }

    @Override
    protected void bindInsert(PreparedStatement ps, Reservation r) throws SQLException {
        bindColumns(ps, r);
    }

    @Override
    protected void bindUpdate(PreparedStatement ps, Reservation r) throws SQLException {
        bindColumns(ps, r);
        ps.setInt(6, r.getId());
    }

    private void bindColumns(PreparedStatement ps, Reservation r) throws SQLException {
        ps.setInt(1, r.getItemId());
        ps.setInt(2, r.getMemberId());
        ps.setString(3, r.getReservedOn().toString());
        ps.setString(4, r.getStatus().name());
        ps.setString(5, r.getHoldUntil() == null ? null : r.getHoldUntil().toString());
    }

    @Override
    protected int idOf(Reservation r) {
        return r.getId();
    }

    @Override
    protected void assignId(Reservation r, int id) {
        r.setId(id);
    }
}
