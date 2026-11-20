package com.ucs553.library.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

class MutableClock extends Clock {

    private Instant now;

    MutableClock(LocalDate day) {
        this.now = day.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

    void advanceDays(long days) {
        now = now.plusSeconds(days * 86400);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
