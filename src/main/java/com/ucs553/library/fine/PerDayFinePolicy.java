package com.ucs553.library.fine;

import com.ucs553.library.model.ItemType;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

public class PerDayFinePolicy implements FinePolicy {

    private final Map<ItemType, BigDecimal> ratePerDay = new EnumMap<>(ItemType.class);

    public PerDayFinePolicy() {
        ratePerDay.put(ItemType.BOOK, new BigDecimal("5.00"));
        ratePerDay.put(ItemType.MAGAZINE, new BigDecimal("3.00"));
        ratePerDay.put(ItemType.DIGITAL, new BigDecimal("1.00"));
    }

    public PerDayFinePolicy(Map<ItemType, BigDecimal> rates) {
        ratePerDay.putAll(rates);
    }

    @Override
    public BigDecimal calculate(ItemType type, long overdueDays) {
        if (overdueDays <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal rate = ratePerDay.getOrDefault(type, BigDecimal.ZERO);
        return rate.multiply(BigDecimal.valueOf(overdueDays)).setScale(2);
    }
}
