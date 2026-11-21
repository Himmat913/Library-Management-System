package com.ucs553.library.service;

import com.ucs553.library.model.Reservation;

public record ReservationDetail(Reservation reservation, String itemTitle, String memberName, int position) {
}
