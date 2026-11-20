package com.ucs553.library.dao;

import com.ucs553.library.exception.LibraryException;

public interface TransactionRunner {

    <T> T run(TransactionWork<T> work) throws LibraryException;
}
