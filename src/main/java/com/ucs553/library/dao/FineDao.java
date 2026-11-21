package com.ucs553.library.dao;

import com.ucs553.library.model.Fine;

import java.util.List;

public interface FineDao extends Dao<Fine> {

    List<Fine> findByMember(int memberId) throws DataAccessException;

    List<Fine> findUnpaid() throws DataAccessException;
}
