package com.ucs553.library.dao;

import com.ucs553.library.model.Staff;

import java.util.Optional;

public interface StaffDao extends Dao<Staff> {

    Optional<Staff> findByUsername(String username) throws DataAccessException;

    int countAdmins() throws DataAccessException;
}
