package com.ucs553.library.dao;

import com.ucs553.library.model.Reservation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReservationDao extends Dao<Reservation> {

    List<Reservation> findWaitingByItem(int itemId) throws DataAccessException;

    List<Reservation> findOpen() throws DataAccessException;

    List<Reservation> findByMember(int memberId) throws DataAccessException;

    Optional<Reservation> findOpenFor(int itemId, int memberId) throws DataAccessException;

    List<Reservation> findLapsedHolds(LocalDate today) throws DataAccessException;
}
