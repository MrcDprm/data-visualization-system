package com.mrcdprm.dataviz.chart;

import java.time.LocalDate;

/** Tarih sütunu X ekseninde olduğunda tarihlerin gruplanması: gün, ay, çeyrek ya da yıl. */
public enum DateBucket {
    DAY,
    MONTH,
    QUARTER,
    YEAR;

    /** Tarihin düştüğü grubun ilk günü: ay için ayın 1'i, çeyrek için çeyreğin ilk ayının 1'i. */
    public LocalDate start(LocalDate d) {
        return switch (this) {
            case DAY -> d;
            case MONTH -> d.withDayOfMonth(1);
            case QUARTER -> LocalDate.of(d.getYear(), (d.getMonthValue() - 1) / 3 * 3 + 1, 1);
            case YEAR -> LocalDate.of(d.getYear(), 1, 1);
        };
    }
}
