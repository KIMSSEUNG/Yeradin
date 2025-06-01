package com.ssafy.dto.contenttype;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentTypeDto {
    private Integer contentTypeId; // DB의 content_type_id와 매핑
    private String contentTypeName; // DB의 content_type_name과 매핑
}