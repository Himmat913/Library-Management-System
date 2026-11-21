package com.ucs553.library.dao;

import com.ucs553.library.model.Notification;

import java.util.List;

public interface NotificationDao extends Dao<Notification> {

    List<Notification> findByMember(int memberId) throws DataAccessException;

    int countUnread(int memberId) throws DataAccessException;

    void markAllRead(int memberId) throws DataAccessException;
}
