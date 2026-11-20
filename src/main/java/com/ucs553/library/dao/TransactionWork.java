package com.ucs553.library.dao;

import com.ucs553.library.exception.LibraryException;

@FunctionalInterface
public interface TransactionWork<T> {

    T execute() throws LibraryException;
}
