package com.ucs553.library.dao;

import java.sql.Connection;
import java.sql.SQLException;

public interface ConnectionSource {

    Connection getConnection() throws SQLException;
}
