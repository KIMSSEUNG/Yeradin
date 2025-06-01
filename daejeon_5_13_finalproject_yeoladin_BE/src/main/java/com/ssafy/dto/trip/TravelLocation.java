package com.ssafy.dto.trip;

import lombok.Data;

@Data
public class TravelLocation {
    private String title;
    private double lat;
    private double lng;
    private String durationFromPrev; // nullable (첫 장소엔 없음)
}
