package com.example.FinanceTracker.security;

import com.example.FinanceTracker.exception.AuthenticationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

	public Long getCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
			throw new AuthenticationException("Authenticated user not found");
		}
		return securityUser.getId();
	}

	public String getCurrentUserEmail() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser securityUser)) {
			throw new AuthenticationException("Authenticated user not found");
		}
		return securityUser.getUsername();
	}
}
