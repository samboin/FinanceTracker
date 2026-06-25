package com.example.FinanceTracker.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {

	private String accessToken;
	private String refreshToken;
	private String tokenType;
	private Long userId;
	private String email;
	private String fullName;
}
