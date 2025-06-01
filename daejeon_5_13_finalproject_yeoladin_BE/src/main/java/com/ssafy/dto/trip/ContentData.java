package com.ssafy.dto.trip;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ContentData {
    private double latitude; //y
    private double longitude; //x
    private String image;
    private String title;
}
