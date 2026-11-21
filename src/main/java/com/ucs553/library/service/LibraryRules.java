package com.ucs553.library.service;

import com.ucs553.library.config.AppConfig;
import com.ucs553.library.config.ConfigException;

import java.math.BigDecimal;

public record LibraryRules(int holdDays, int maxRenewals, BigDecimal fineBlockLimit) {

    public static LibraryRules defaults() {
        return new LibraryRules(3, 1, BigDecimal.valueOf(100));
    }

    public static LibraryRules from(AppConfig config) throws ConfigException {
        return new LibraryRules(config.getInt("reservation.holdDays"), config.getInt("loan.maxRenewals"),
                BigDecimal.valueOf(config.getInt("fine.blockLimit")));
    }
}
