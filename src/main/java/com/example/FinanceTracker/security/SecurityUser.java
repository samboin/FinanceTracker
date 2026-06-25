package com.example.FinanceTracker.security;

import com.example.FinanceTracker.entity.User;
import com.example.FinanceTracker.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityUser implements UserDetails {

	private final Long id;
	private final String email;
	private final String password;
	private final UserRole role;

	public SecurityUser(Long id, String email, String password, UserRole role) {
		this.id = id;
		this.email = email;
		this.password = password;
		this.role = role;
	}

	public static SecurityUser from(User user) {
		return new SecurityUser(user.getId(), user.getEmail(), user.getPassword(), user.getRole());
	}

	public Long getId() {
		return id;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getUsername() {
		return email;
	}
}
