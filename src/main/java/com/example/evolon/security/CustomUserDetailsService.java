package com.example.evolon.security;

import java.util.List;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository users; // User エンティティ操作用リポジトリ

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

		User u = users.findByEmailIgnoreCase(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		if (u.isBanned()) {
			throw new DisabledException("Account banned");
		}
		if (!u.isEnabled()) {
			throw new DisabledException("Account disabled");
		}

		return new org.springframework.security.core.userdetails.User(
				u.getEmail(),
				u.getPassword(),
				List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole())));
	}
}
