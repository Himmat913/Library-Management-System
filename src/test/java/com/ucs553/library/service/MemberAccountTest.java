package com.ucs553.library.service;

import com.ucs553.library.dao.Database;
import com.ucs553.library.dao.JdbcMemberAccountDao;
import com.ucs553.library.dao.JdbcMemberDao;
import com.ucs553.library.dao.JdbcStaffDao;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemberAccountTest {

    @TempDir
    Path dir;

    private AuthService auth;
    private Member member;

    @BeforeEach
    void setUp() throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        JdbcMemberDao members = new JdbcMemberDao(db);
        member = new Member("Priya Sharma", "priya@example.com");
        members.insert(member);
        auth = new AuthService(new JdbcStaffDao(db), members, new JdbcMemberAccountDao(db));
    }

    @Test
    void temporaryPasswordWorksOnceAndMustBeChanged() throws Exception {
        assertFalse(auth.hasAccount(member.getId()));
        String temporary = auth.createOrResetMemberAccount(member.getId());
        assertEquals(8, temporary.length());
        assertTrue(auth.hasAccount(member.getId()));

        Optional<MemberLogin> login = auth.loginMember("PRIYA@example.com", temporary.toCharArray());
        assertTrue(login.isPresent());
        assertTrue(login.get().mustChangePassword());
        assertEquals(member.getId(), login.get().member().getId());
    }

    @Test
    void wrongPasswordUnknownEmailAndMissingAccountAreRefused() throws Exception {
        assertTrue(auth.loginMember("priya@example.com", "whatever1".toCharArray()).isEmpty());
        String temporary = auth.createOrResetMemberAccount(member.getId());
        assertTrue(auth.loginMember("priya@example.com", (temporary + "x").toCharArray()).isEmpty());
        assertTrue(auth.loginMember("nobody@example.com", temporary.toCharArray()).isEmpty());
    }

    @Test
    void changingThePasswordClearsTheFlagAndReplacesTheOldPassword() throws Exception {
        String temporary = auth.createOrResetMemberAccount(member.getId());
        auth.changeMemberPassword(member, temporary.toCharArray(), "brandnew1".toCharArray());

        assertTrue(auth.loginMember("priya@example.com", temporary.toCharArray()).isEmpty());
        Optional<MemberLogin> login = auth.loginMember("priya@example.com", "brandnew1".toCharArray());
        assertTrue(login.isPresent());
        assertFalse(login.get().mustChangePassword());
    }

    @Test
    void changeNeedsTheCurrentPasswordAndAnAcceptableNewOne() throws Exception {
        String temporary = auth.createOrResetMemberAccount(member.getId());
        assertThrows(LibraryException.class,
                () -> auth.changeMemberPassword(member, "wrong".toCharArray(), "brandnew1".toCharArray()));
        assertThrows(LibraryException.class,
                () -> auth.changeMemberPassword(member, temporary.toCharArray(), "abc".toCharArray()));
    }

    @Test
    void resettingGivesANewTemporaryPassword() throws Exception {
        String first = auth.createOrResetMemberAccount(member.getId());
        String second = auth.createOrResetMemberAccount(member.getId());
        assertNotEquals(first, second);
        assertTrue(auth.loginMember("priya@example.com", first.toCharArray()).isEmpty());
        assertTrue(auth.loginMember("priya@example.com", second.toCharArray()).isPresent());
        assertEquals(1, auth.membersWithAccounts().size());
    }
}
