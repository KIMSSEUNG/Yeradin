package com.ssafy.dto.shortform;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortformContentTypeMappingDto {
    private Integer shortformPk;
    private Integer contentTypeId; // 혹은 String contentTypeName; (매퍼 구현에 따라 달라짐)
    private String contentTypeName; // 매퍼에서 contenttypes와 조인해서 가져오는 경우
}