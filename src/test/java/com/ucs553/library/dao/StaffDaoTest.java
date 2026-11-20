package com.ucs553.library.dao;

import com.ucs553.library.model.Role;
import com.ucs553.library.model.Staff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StaffDaoTest {

    @TempDir
    Path dir;

    private StaffDao staff;

    @BeforeEach
    void setUp() throws Exception {
        staff = new JdbcStaffDao(TestDb.create(dir));
    }

    @Test
    void roundTripKeepsRoleAndHash() throws Exception {
        staff.insert(new Staff("Admin", "a@library.local", "admin", Role.ADMIN, "hash-1"));
        Staff loaded = staff.findByUsername("ADMIN").orElseThrow();
        assertEquals(Role.ADMIN, loaded.getRole());
        assertEquals("hash-1", loaded.getPasswordHash());
    }

    @Test
    void usernameAndEmailMustBeUnique() throws Exception {
        staff.insert(new Staff("A", "a@library.local", "alpha", Role.LIBRARIAN, "h"));
        assertThrows(DataAccessException.class,
                () -> staff.insert(new Staff("B", "b@library.local", "Alpha", Role.LIBRARIAN, "h")));
        assertThrows(DataAccessException.class,
                () -> staff.insert(new Staff("C", "a@library.local", "gamma", Role.LIBRARIAN, "h")));
    }

    @Test
    void countsOnlyAdmins() throws Exception {
        staff.insert(new Staff("A", "a@library.local", "alpha", Role.ADMIN, "h"));
        staff.insert(new Staff("B", "b@library.local", "beta", Role.LIBRARIAN, "h"));
        assertEquals(1, staff.countAdmins());
    }
}
