package com.example.evolon.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.evolon.entity.User;
import com.example.evolon.repository.UserRepository;

@Configuration
public class InitAdminRunner {

	@Bean
	CommandLineRunner initAdmin(UserRepository users, PasswordEncoder encoder) {
		return args -> {

			// -----------------------------------------
			// ① ADMIN の作成
			// -----------------------------------------
			String adminEmail = "admin@evolon.com";
			if (users.findByEmailIgnoreCase(adminEmail).isEmpty()) {

				User admin = new User();
				admin.setEmail(adminEmail);
				admin.setName("Admin");
				admin.setPassword(encoder.encode("admin123"));
				admin.setRole("ADMIN");
				admin.setEnabled(true);

				users.save(admin);

				System.out.println("✅ 初期ADMINユーザーを作成しました");
			}

			// -----------------------------------------
			// ② テストユーザー Member1〜3 の作成
			// -----------------------------------------
			createTestUser(users, encoder,
					"member1@evolon.com", "Member1", "test123");

			createTestUser(users, encoder,
					"member2@evolon.com", "Member2", "test123");

			createTestUser(users, encoder,
					"member3@evolon.com", "Member3", "test123");

			System.out.println("✅ テストユーザー1〜3を作成しました");
		};
	}

	private void createTestUser(UserRepository users, PasswordEncoder encoder,
			String email, String name, String rawPassword) {
		if (users.findByEmailIgnoreCase(email).isPresent())
			return;

		User u = new User();
		u.setEmail(email);
		u.setName(name);
		u.setPassword(encoder.encode(rawPassword));
		u.setRole("USER");
		u.setEnabled(true);
		users.save(u);
	}
}
