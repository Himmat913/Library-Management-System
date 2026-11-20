package com.ucs553.library.dao;

import com.ucs553.library.model.Member;

import java.util.List;
import java.util.Optional;

public interface MemberDao extends Dao<Member> {

    Optional<Member> findByEmail(String email) throws DataAccessException;

    List<Member> search(String text) throws DataAccessException;
}
