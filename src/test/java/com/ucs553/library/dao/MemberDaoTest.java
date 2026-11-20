package com.ucs553.library.dao;

import com.ucs553.library.model.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class MemberDaoTest {

    @TempDir
    Path dir;

    private MemberDao members;

    @BeforeEach
    void setUp() throws Exception {
        members = new JdbcMemberDao(TestDb.create(dir));
    }

    @Test
    void insertAndFindByEmail() throws Exception {
        members.insert(new Member("Himmat Sidhu", "Himmat@Example.com"));
        Member found = members.findByEmail("himmat@example.com").orElseThrow();
        assertEquals("Himmat Sidhu", found.getName());
        assertEquals(5, found.getBorrowLimit());
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        members.insert(new Member("A", "a@example.com"));
        assertThrows(DataAccessException.class, () -> members.insert(new Member("B", "a@example.com")));
    }

    @Test
    void updateChangesStoredValues() throws Exception {
        Member m = new Member("A", "a@example.com");
        members.insert(m);
        Member edited = new Member("A Kaur", "a@example.com", m.getJoinedOn(), 8);
        edited.setId(m.getId());
        members.update(edited);
        assertEquals(8, members.findById(m.getId()).orElseThrow().getBorrowLimit());
    }

    @Test
    void searchFindsByName() throws Exception {
        members.insert(new Member("Priya Sharma", "p@example.com"));
        members.insert(new Member("Rahul Verma", "r@example.com"));
        assertEquals(1, members.search("priya").size());
    }
}
