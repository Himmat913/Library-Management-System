package com.ucs553.library.dao;

import com.ucs553.library.model.MemberAccount;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class JdbcMemberAccountDao implements MemberAccountDao {

    private final ConnectionSource source;

    public JdbcMemberAccountDao(ConnectionSource source) {
        this.source = source;
    }

    @Override
    public Optional<MemberAccount> find(int memberId) throws DataAccessException {
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM member_accounts WHERE member_id = ?")) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new MemberAccount(rs.getInt("member_id"), rs.getString("password_hash"),
                            rs.getInt("must_change") == 1));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not read the member account", e);
        }
    }

    @Override
    public void save(MemberAccount account) throws DataAccessException {
        String sql = "INSERT INTO member_accounts (member_id, password_hash, must_change) VALUES (?,?,?) "
                + "ON CONFLICT(member_id) DO UPDATE SET password_hash = excluded.password_hash, "
                + "must_change = excluded.must_change";
        try (Connection c = source.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, account.memberId());
            ps.setString(2, account.passwordHash());
            ps.setInt(3, account.mustChange() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Could not save the member account", e);
        }
    }

    @Override
    public Set<Integer> memberIdsWithAccounts() throws DataAccessException {
        try (Connection c = source.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT member_id FROM member_accounts");
             ResultSet rs = ps.executeQuery()) {
            Set<Integer> ids = new HashSet<>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
            }
            return ids;
        } catch (SQLException e) {
            throw new DataAccessException("Could not list the member accounts", e);
        }
    }
}
