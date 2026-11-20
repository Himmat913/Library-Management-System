package com.ucs553.library.service;

import com.ucs553.library.dao.Database;
import com.ucs553.library.dao.JdbcStaffDao;
import com.ucs553.library.exception.LibraryException;
import com.ucs553.library.model.Role;
import com.ucs553.library.model.Staff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    @TempDir
    Path dir;

    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        Database db = new Database("jdbc:sqlite:" + dir.resolve("test.db"));
        db.initSchema();
        auth = new AuthService(new JdbcStaffDao(db));
        auth.ensureDefaultAdmin();
    }

    @Test
    void defaultAdminIsCreatedOnlyOnce() throws Exception {
        assertFalse(auth.ensureDefaultAdmin());
        assertEquals(1, auth.allStaff().size());
        assertTrue(auth.defaultAdminStillUsed());
    }

    @Test
    void loginChecksUsernameAndPassword() throws Exception {
        assertTrue(auth.login("admin", "admin123".toCharArray()).isPresent());
        assertTrue(auth.login("ADMIN", "admin123".toCharArray()).isPresent());
        assertTrue(auth.login("admin", "wrong".toCharArray()).isEmpty());
        assertTrue(auth.login("nobody", "admin123".toCharArray()).isEmpty());
    }

    @Test
    void createdStaffCanSignIn() throws Exception {
        auth.createStaff("Priya", "priya@library.local", "priya", Role.LIBRARIAN, "pass1234".toCharArray());
        Optional<Staff> staff = auth.login("priya", "pass1234".toCharArray());
        assertTrue(staff.isPresent());
        assertFalse(staff.get().isAdmin());
    }

    @Test
    void duplicateUsernameOrShortPasswordIsRefused() throws Exception {
        assertThrows(LibraryException.class,
                () -> auth.createStaff("X", "x@library.local", "admin", Role.LIBRARIAN, "pass1234".toCharArray()));
        assertThrows(LibraryException.class,
                () -> auth.createStaff("X", "x@library.local", "xavier", Role.LIBRARIAN, "123".toCharArray()));
    }

    @Test
    void changingPasswordNeedsTheOldOne() throws Exception {
        Staff admin = auth.login("admin", "admin123".toCharArray()).orElseThrow();
        assertThrows(LibraryException.class,
                () -> auth.changePassword(admin, "nope".toCharArray(), "newpass1".toCharArray()));
        auth.changePassword(admin, "admin123".toCharArray(), "newpass1".toCharArray());
        assertTrue(auth.login("admin", "admin123".toCharArray()).isEmpty());
        assertTrue(auth.login("admin", "newpass1".toCharArray()).isPresent());
        assertFalse(auth.defaultAdminStillUsed());
    }

    @Test
    void adminsCannotRemoveThemselvesOrTheLastAdmin() throws Exception {
        Staff admin = auth.login("admin", "admin123".toCharArray()).orElseThrow();
        assertThrows(LibraryException.class, () -> auth.deleteStaff(admin.getId(), admin));

        Staff other = auth.createStaff("Second", "second@library.local", "second", Role.ADMIN, "pass1234".toCharArray());
        auth.deleteStaff(admin.getId(), other);
        assertThrows(LibraryException.class, () -> auth.deleteStaff(other.getId(), admin));
    }
}
