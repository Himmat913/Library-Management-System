package com.ucs553.library.fine;

import com.ucs553.library.model.ItemType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FinePolicyTest {

    private final FinePolicy policy = new PerDayFinePolicy();

    @Test
    void noFineWhenNotOverdue() {
        assertEquals(new BigDecimal("0.00"), policy.calculate(ItemType.BOOK, 0));
    }

    @Test
    void rateDependsOnItemType() {
        assertEquals(new BigDecimal("25.00"), policy.calculate(ItemType.BOOK, 5));
        assertEquals(new BigDecimal("15.00"), policy.calculate(ItemType.MAGAZINE, 5));
        assertEquals(new BigDecimal("5.00"), policy.calculate(ItemType.DIGITAL, 5));
    }
}
