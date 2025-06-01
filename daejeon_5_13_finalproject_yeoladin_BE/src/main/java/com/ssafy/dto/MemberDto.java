package com.ssafy.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberDto {
	private Integer pk;
	private Integer id;
	private String name;
	private String email;
	private String pw;
	private String provider;
	private String provider_id;
	private String role;
}
