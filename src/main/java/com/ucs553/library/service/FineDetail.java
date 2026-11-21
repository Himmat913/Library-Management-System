package com.ucs553.library.service;

import com.ucs553.library.model.Fine;

public record FineDetail(Fine fine, String memberName, String itemTitle) {
}
