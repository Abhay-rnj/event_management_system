package com.eventmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Authentication and authorization rules.
 *
 * SESSION vs JWT: this project uses classic session login (a cookie holds the session id, the server keeps
 * the session). It is the simplest and safest choice for a server-rendered Thymeleaf site, and Spring Security
 * gives CSRF protection for free. JWT is better when you build a separate mobile app or a React front end
 * that calls a JSON API. If you add one later, create /api/** endpoints, issue a signed JWT from a /api/login
 * endpoint, and validate it in a filter. The rest of the app (services and repositories) stays the same.
 */
@Configuration
public class SecurityConfig {

    /** BCrypt: salted, slow-by-design hashing. Passwords are never stored or compared in plain text. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // public: styles, banner images, browsing events, login and password flows
                .requestMatchers("/css/**", "/uploads/**", "/error").permitAll()
                .requestMatchers("/", "/events", "/events/{id}", "/login", "/signup", "/verify",
                        "/resend-verification", "/forgot", "/reset").permitAll()
                // the payment gateway calls this itself, so it can't have a login. It is protected by a signature instead.
                .requestMatchers("/payments/webhook").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/organizer/**").hasAnyRole("ORGANIZER", "ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", false)   // false = go back to the page the user wanted
                .failureHandler((request, response, exception) -> {
                    // DbUserDetailsService marks unverified users as "disabled" and suspended users as "locked"
                    String reason = exception instanceof DisabledException ? "unverified"
                            : exception instanceof LockedException ? "suspended" : "error";
                    response.sendRedirect(request.getContextPath() + "/login?" + reason);
                }))
            .logout(logout -> logout.logoutSuccessUrl("/login?loggedOut"))
            // CSRF protection stays ON for every form. Only the webhook is exempt: the gateway can't send our token.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/payments/webhook"));
        return http.build();
    }
}
