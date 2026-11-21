package com.ucs553.library.dao;

import com.ucs553.library.model.MemberAccount;

import java.util.Optional;
import java.util.Set;

public interface MemberAccountDao {

    Optional<MemberAccount> find(int memberId) throws DataAccessException;

    void save(MemberAccount account) throws DataAccessException;

    Set<Integer> memberIdsWithAccounts() throws DataAccessException;
}
