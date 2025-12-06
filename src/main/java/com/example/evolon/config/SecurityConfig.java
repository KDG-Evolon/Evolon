package com.example.evolon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.evolon.repository.UserRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	// セキュリティ設定
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/login", "/css/**", "/js/**", "/images/**", "/items/**")
						.permitAll()
						.requestMatchers("/orders/stripe-webhook").permitAll()
						.requestMatchers("/admin/**").hasRole("ADMIN")
						.anyRequest().authenticated())
				.formLogin(form -> form
						.loginPage("/login")
						.defaultSuccessUrl("/items", true)
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout")
						.permitAll())
				.csrf(csrf -> csrf
						.ignoringRequestMatchers("/orders/stripe-webhook"));

		return http.build();
	}

	// DB からユーザをロード
	@Bean
	public UserDetailsService userDetailsService(UserRepository userRepository) {
		return email -> userRepository.findByEmail(email)
				.map(user -> org.springframework.security.core.userdetails.User.builder()
						.username(user.getEmail())
						.password(user.getPassword())
						.roles(user.getRole())
						.disabled(!user.isEnabled())
						.build())
				.orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
	}

	// パスワードエンコーダ
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
