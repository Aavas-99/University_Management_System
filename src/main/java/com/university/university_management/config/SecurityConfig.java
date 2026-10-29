package com.university.university_management.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import tools.jackson.databind.ObjectMapper;
import com.university.university_management.repository.UniversityUserRepository;

@Configuration
public class SecurityConfig {
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(UniversityUserRepository repository) {
		return email -> repository.findByEmail(email)
				.map(account -> User.withUsername(account.getEmail())
						.password(account.getPasswordHash())
						.roles(account.getRole().name())
				.disabled(!account.isActive())
				.build())
				.orElseThrow(() -> new UsernameNotFoundException("Account not found"));
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		AuthenticationSuccessHandler successHandler = (request, response, authentication) -> {
			if (acceptsHtml(request)) {
				response.sendRedirect("/");
				return;
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getOutputStream(),
					new LoginResponse("Login successful", authentication.getName()));
		};
		AuthenticationFailureHandler failureHandler = (request, response, exception) -> {
			if (acceptsHtml(request)) {
				response.sendRedirect("/login?error");
				return;
			}
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getOutputStream(),
					new ErrorResponse("Invalid email or password"));
		};

		http
				.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/", "/login", "/error", "/api/auth/csrf", "/api/auth/login").permitAll()
						.requestMatchers("/records/admins", "/records/admins/**").hasRole("ADMIN")
						.requestMatchers("/records/teachers", "/records/teachers/**")
								.hasAnyRole("ADMIN", "TEACHER")
						.requestMatchers("/records/students", "/records/students/**")
								.hasAnyRole("ADMIN", "TEACHER", "STUDENT")
						.requestMatchers("/api/profile", "/api/dashboard").authenticated()
						.requestMatchers("/api/accounts/**").hasRole("ADMIN")
						.requestMatchers("/api/admins", "/api/admins/**").hasRole("ADMIN")
						.requestMatchers("/api/teachers", "/api/teachers/**").hasAnyRole("ADMIN", "TEACHER")
						.requestMatchers(HttpMethod.GET, "/api/students", "/api/students/**")
								.hasAnyRole("ADMIN", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.POST, "/api/students", "/api/students/**")
								.hasAnyRole("ADMIN", "STUDENT")
						.requestMatchers(HttpMethod.PUT, "/api/students", "/api/students/**")
								.hasAnyRole("ADMIN", "TEACHER", "STUDENT")
						.requestMatchers(HttpMethod.DELETE, "/api/students", "/api/students/**")
								.hasAnyRole("ADMIN", "TEACHER", "STUDENT")
						.requestMatchers("/api/**").authenticated()
						.anyRequest().permitAll())
				.exceptionHandling(exceptions -> exceptions
						.authenticationEntryPoint((request, response, exception) -> {
							if (request.getRequestURI().startsWith("/api/")) {
								response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
								response.setContentType(MediaType.APPLICATION_JSON_VALUE);
								objectMapper.writeValue(response.getOutputStream(),
										new ErrorResponse("Authentication is required"));
							}
							else {
								response.sendRedirect("/login");
							}
						})
						.accessDeniedHandler((request, response, exception) -> {
							if (acceptsHtml(request)) {
								response.sendRedirect("/");
								return;
							}
							response.setStatus(HttpServletResponse.SC_FORBIDDEN);
							response.setContentType(MediaType.APPLICATION_JSON_VALUE);
							objectMapper.writeValue(response.getOutputStream(),
									new ErrorResponse("You do not have permission to access this resource"));
						}))
				.formLogin(form -> form
						.loginPage("/login")
						.loginProcessingUrl("/api/auth/login")
						.successHandler(successHandler)
						.failureHandler(failureHandler)
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/api/auth/logout")
						.logoutSuccessHandler((request, response, authentication) -> {
							if (acceptsHtml(request)) {
								response.sendRedirect("/");
							}
							else {
								response.setStatus(HttpServletResponse.SC_NO_CONTENT);
							}
						})
						.permitAll());
		return http.build();
	}

	private boolean acceptsHtml(HttpServletRequest request) {
		String accept = request.getHeader("Accept");
		return accept != null && accept.contains(MediaType.TEXT_HTML_VALUE);
	}

	private record LoginResponse(String message, String email) {
	}

	private record ErrorResponse(String error) {
	}
}
