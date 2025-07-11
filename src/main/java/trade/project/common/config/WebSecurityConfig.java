package trade.project.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API이므로)
            .csrf(AbstractHttpConfigurer::disable)
            // 요청별 권한 설정
            .authorizeHttpRequests(authz -> authz
                // 공개 API (인증 불필요)
                .requestMatchers(
                        "/uapi/**",
                        "/oauth2/**",
                        "/api/**",
                    "/api/users/signup",
                    "/api/users/login",
                    "/api/users/check-username",
                    "/api/users/check-email",
                    "/api/test/**",
                    "/actuator/**",
                    "/api/news"
                ).permitAll()
                
                // 나머지 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            
            // 기본 로그인/로그아웃 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
} 