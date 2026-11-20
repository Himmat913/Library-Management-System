package com.ucs553.library.dao;

import java.util.List;
import java.util.Optional;

public interface Dao<T> {

    int insert(T entity) throws DataAccessException;

    void update(T entity) throws DataAccessException;

    void delete(int id) throws DataAccessException;

    Optional<T> findById(int id) throws DataAccessException;

    List<T> findAll() throws DataAccessException;
}
