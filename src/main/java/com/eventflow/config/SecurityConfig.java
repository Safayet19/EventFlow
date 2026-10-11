package com.eventflow.config;

import com.eventflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepository) {
        return email -> userRepository.findByEmailIgnoreCase(email)
                .map(user -> org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            @Value("${eventflow.security.remember-key:eventflow-local-remember-key}") String rememberKey) throws Exception {
        // Keep a stable configured key in deployment. If missing, generate a unique
        // per-start key instead of accepting the predictable development fallback.
        String effectiveRememberKey = rememberKey;
        if (production() && "eventflow-local-remember-key".equals(rememberKey)) {
            byte[] entropy = new byte[32];
            new java.security.SecureRandom().nextBytes(entropy);
            effectiveRememberKey = java.util.Base64.getEncoder().encodeToString(entropy);
        }
        final String signingKey = effectiveRememberKey;

        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/home", "/login", "/register", "/forgot-password/**", "/forgot-password", "/reset-password", "/contact", "/events", "/events/**", "/event-images/**", "/event-gallery-images/**", "/speaker-images/**", "/sponsor-images/**", "/error", "/favicon.ico", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/organizer/**").hasRole("ORGANIZER")
                        .requestMatchers("/participant/**").hasRole("PARTICIPANT")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .failureHandler((request, response, exception) -> {
                            String redirect = request.getParameter("redirect");
                            String target = "/login?error";
                            String safeRedirect = safeRedirect(redirect);
                            if (safeRedirect != null) {
                                target += "&redirect=" + java.net.URLEncoder.encode(safeRedirect, java.nio.charset.StandardCharsets.UTF_8);
                            }
                            response.sendRedirect(target);
                        })
                        .successHandler((request, response, authentication) -> {
                            String redirect = request.getParameter("redirect");
                            String safeRedirect = safeRedirect(redirect);
                            if (safeRedirect != null) {
                                response.sendRedirect(safeRedirect);
                            } else {
                                response.sendRedirect("/dashboard");
                            }
                        })
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key(signingKey)
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/access-denied")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            if ("register".equals(request.getParameter("next"))) {
                                response.sendRedirect("/register");
                            } else {
                                response.sendRedirect("/login?logout");
                            }
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                );

        return http.build();
    }

    /** Same redirect protection as before, without a separate utility class. */
    public static String safeRedirect(String candidate) {
        if (candidate == null) return null;
        String cleaned = candidate.trim();
        if (!cleaned.startsWith("/") || cleaned.startsWith("//") || cleaned.contains("\\")
                || cleaned.contains("\r") || cleaned.contains("\n") || cleaned.contains("..")
                || cleaned.toLowerCase(java.util.Locale.ROOT).contains("%5c")
                || cleaned.toLowerCase(java.util.Locale.ROOT).contains("%2f")) return null;
        try {
            java.net.URI uri = java.net.URI.create(cleaned);
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getUserInfo() != null) return null;
            return cleaned;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Preserve the same hosted/production detection without another Java class. */
    public static boolean production() {
        String profile = System.getProperty("spring.profiles.active");
        if (profile == null || profile.isBlank()) profile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (profile != null) {
            for (String item : profile.toLowerCase(java.util.Locale.ROOT).split(",")) {
                if (item.trim().equals("prod") || item.trim().equals("production")) return true;
            }
        }
        String port = System.getenv("PORT");
        return "true".equalsIgnoreCase(System.getenv("RENDER"))
                || (port != null && !port.isBlank());
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
