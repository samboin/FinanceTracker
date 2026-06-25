package com.example.FinanceTracker.service;

import com.example.FinanceTracker.dto.request.LoginRequest;
import com.example.FinanceTracker.dto.request.RefreshTokenRequest;
import com.example.FinanceTracker.dto.request.RegisterRequest;
import com.example.FinanceTracker.dto.response.AuthResponse;
import com.example.FinanceTracker.entity.RefreshToken;
import com.example.FinanceTracker.entity.User;
import com.example.FinanceTracker.entity.UserRole;
import com.example.FinanceTracker.exception.AuthenticationException;
import com.example.FinanceTracker.exception.DuplicateResourceException;
import com.example.FinanceTracker.repository.RefreshTokenRepository;
import com.example.FinanceTracker.repository.UserRepository;
import com.example.FinanceTracker.security.JwtService;
import com.example.FinanceTracker.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;
	private final AuthenticationManager authenticationManager;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.getEmail())) {
			throw new DuplicateResourceException("User already exists with email: " + request.getEmail());
		}

		User user = new User();
		user.setFullName(request.getFullName());
		user.setEmail(request.getEmail());
		user.setPassword(passwordEncoder.encode(request.getPassword()));
		user.setRole(UserRole.USER);

		User savedUser = userRepository.save(user);
		return buildAuthResponse(savedUser);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
			);
		} catch (BadCredentialsException ex) {
			throw new AuthenticationException("Invalid email or password");
		}

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> new AuthenticationException("User not found with email: " + request.getEmail()));

		return buildAuthResponse(user);
	}

	@Transactional
	public AuthResponse refresh(RefreshTokenRequest request) {
		RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
				.orElseThrow(() -> new AuthenticationException("Refresh token is invalid"));

		if (storedToken.isRevoked()) {
			throw new AuthenticationException("Refresh token has been revoked");
		}

		if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
			throw new AuthenticationException("Refresh token has expired");
		}

		SecurityUser securityUser = SecurityUser.from(storedToken.getUser());
		if (!jwtService.isTokenValid(storedToken.getToken(), securityUser)) {
			throw new AuthenticationException("Refresh token is invalid");
		}

		String accessToken = jwtService.generateAccessToken(securityUser);
		return AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(storedToken.getToken())
				.tokenType("Bearer")
				.userId(storedToken.getUser().getId())
				.email(storedToken.getUser().getEmail())
				.fullName(storedToken.getUser().getFullName())
				.build();
	}

	@Transactional
	public void logout(RefreshTokenRequest request) {
		RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
				.orElseThrow(() -> new AuthenticationException("Refresh token is invalid"));
		storedToken.setRevoked(true);
	}

	private AuthResponse buildAuthResponse(User user) {
		SecurityUser securityUser = SecurityUser.from(user);
		String accessToken = jwtService.generateAccessToken(securityUser);
		String refreshToken = jwtService.generateRefreshToken(securityUser);

		refreshTokenRepository.deleteByUser(user);

		RefreshToken tokenEntity = new RefreshToken();
		tokenEntity.setToken(refreshToken);
		tokenEntity.setUser(user);
		tokenEntity.setExpiresAt(jwtService.extractExpiration(refreshToken).atZone(java.time.ZoneOffset.UTC).toLocalDateTime());
		tokenEntity.setRevoked(false);
		refreshTokenRepository.save(tokenEntity);

		return AuthResponse.builder()
				.accessToken(accessToken)
				.refreshToken(refreshToken)
				.tokenType("Bearer")
				.userId(user.getId())
				.email(user.getEmail())
				.fullName(user.getFullName())
				.build();
	}
}
