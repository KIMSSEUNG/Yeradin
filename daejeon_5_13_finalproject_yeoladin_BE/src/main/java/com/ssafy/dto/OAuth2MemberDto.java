package com.ssafy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2MemberDto {
//	private Integer pk;
	private Integer id;
	private String name;
	private String email;
	private String provider;
	private String provider_id;
}
