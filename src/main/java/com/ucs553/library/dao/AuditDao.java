package com.ucs553.library.dao;

import com.ucs553.library.model.AuditEntry;

import java.util.List;

public interface AuditDao extends Dao<AuditEntry> {

    List<AuditEntry> findRecent(int limit) throws DataAccessException;
}
