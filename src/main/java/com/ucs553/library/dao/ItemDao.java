package com.ucs553.library.dao;

import com.ucs553.library.model.Item;

import java.util.List;

public interface ItemDao extends Dao<Item> {

    List<Item> search(String text) throws DataAccessException;
}
