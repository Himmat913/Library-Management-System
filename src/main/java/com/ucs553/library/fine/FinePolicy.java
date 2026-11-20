package com.ucs553.library.fine;

import com.ucs553.library.model.ItemType;

import java.math.BigDecimal;

public interface FinePolicy {

    BigDecimal calculate(ItemType type, long overdueDays);
}
