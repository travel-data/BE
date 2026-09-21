package com.nigahae.security.config;

import com.nigahae.security.jwt.JwtAuthenticationFilter;
import com.nigahae.security.oauth.OAuth2LoginSuccessHandler;
import com.nigahae.security.oauth.OAuth2LoginFailureHandler;
import com.nigahae.security.oauth.OAuth2DiagnosticFilter;
import com.nigahae.security.oauth.OAuth2LoginFailureHandler;
import com.nigahae.security.oauth.OAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private static final String FESTIVAL_API_PATH = "/api/v1/event";
    private static final String TOUR_SPOT_API_PATH = "/api/v1/tour-spots";
    private static final String NEARBY_PLACE_API_PATH = "/api/v1/nearby-places";

    private final OAuth2UserService oAuth2UserService;
    private final OAuth2LoginSuccessHandler successHandler;
    private final OAuth2LoginFailureHandler failureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}") private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(storyCardFeaturedReadRequestMatcher()).permitAll()
                        .requestMatchers(
                                "/api/v1/tour-courses/*/password/verify",
                                "/api/v1/tour-courses/*/public",
                                "/api/v1/tour-courses/featured",
                                "/api/v1/recommendations/courses"
                        ).permitAll()
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/v1/tour-courses/**",
                                "/api/v1/users/me/**",
                                "/api/v1/tour-spots/saved",
                                "/api/v1/tour-spots/*/save",
                                "/api/v1/memos/**",
                                "/api/v1/story-cards/**",
                                "/api/v1/tour-spots/*/stamp-missions/**",
                                "/api/v1/tour-spots/nearby-places/*/save"
                        ).authenticated()
                        .requestMatchers(festivalReadRequestMatcher()).permitAll()
                        .requestMatchers(tourSpotReadRequestMatcher()).permitAll()
                        .requestMatchers(nearbyPlaceReadRequestMatcher()).permitAll()
                        .requestMatchers(nearbyPlaceDetailReadRequestMatcher()).permitAll()
                        .requestMatchers(
                                "/actuator/health",
                                "/api/auth/dev-login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/login/**",
                                "/oauth2/**",
                                "/api/auth/kakao",
                                "/error",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/v1/routes/places/calculate",
                                "/internal/routes/calculate",
                                "/internal/tour-data/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception.defaultAuthenticationEntryPointFor(
                        new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/")))
                .oauth2Login(oauth -> oauth.userInfoEndpoint(user -> user.userService(oAuth2UserService))
                        .successHandler(successHandler)
                        .failureHandler(failureHandler))
                .addFilterBefore(new OAuth2DiagnosticFilter(), OAuth2AuthorizationRequestRedirectFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    static RequestMatcher storyCardFeaturedReadRequestMatcher() {
        return request -> HttpMethod.GET.matches(request.getMethod())
                && "/api/v1/story-cards/featured".equals(
                        request.getRequestURI().substring(request.getContextPath().length()));
    }

    static RequestMatcher festivalReadRequestMatcher() {
        return collectionAndDetailGetRequestMatcher(FESTIVAL_API_PATH);
    }

    static RequestMatcher tourSpotReadRequestMatcher() {
        return numericCollectionAndDetailGetRequestMatcher(TOUR_SPOT_API_PATH);
    }

    static RequestMatcher nearbyPlaceReadRequestMatcher() {
        return request -> {
            if (!HttpMethod.GET.matches(request.getMethod())) {
                return false;
            }
            String path = request.getRequestURI().substring(request.getContextPath().length());
            return path.matches("^/api/v1/tour-spots/\\d+/nearby-places$");
        };
    }

    static RequestMatcher nearbyPlaceDetailReadRequestMatcher() {
        return request -> {
            if (!HttpMethod.GET.matches(request.getMethod())) {
                return false;
            }
            String path = request.getRequestURI().substring(request.getContextPath().length());
            return path.matches("^/api/v1/nearby-places/\\d+$");
        };
    }

    private static RequestMatcher numericCollectionAndDetailGetRequestMatcher(String apiPath) {
        return request -> {
            if (!HttpMethod.GET.matches(request.getMethod())) {
                return false;
            }

            String path = request.getRequestURI().substring(request.getContextPath().length());
            return apiPath.equals(path) || path.matches("^" + apiPath + "/\\d+$");
        };
    }

    private static RequestMatcher collectionAndDetailGetRequestMatcher(String apiPath) {
        return request -> {
            if (!HttpMethod.GET.matches(request.getMethod())) {
                return false;
            }

            String path = request.getRequestURI().substring(request.getContextPath().length());
            if (apiPath.equals(path)) {
                return true;
            }

            int detailIdStart = apiPath.length() + 1;
            return path.startsWith(apiPath + "/")
                    && path.length() > detailIdStart
                    && path.indexOf('/', detailIdStart) == -1;
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
