package com.ssafy.dto.trip;

import java.util.List;
import lombok.Data;

@Data
public class TravelPlan {
    private int day;
    private List<TravelLocation> route;
}

