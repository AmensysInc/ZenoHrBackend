package com.application.employee.service.config;

import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

import static com.application.employee.service.user.Permission.*;
import static com.application.employee.service.user.Role.*;
import static org.springframework.http.HttpMethod.*;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:3002",
                "http://localhost:3005",
                "http://204.12.199.212:3005",
                "https://204.12.199.212:3005",
                "http://zenopayhr.com",
                "https://zenopayhr.com",
                "http://www.zenopayhr.com",
                "https://www.zenopayhr.com"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests()
                .requestMatchers("/auth/authenticate","/auth/register","/auth/resetPassword","/auth/updatePassword","/admin/create-user","/admin/create-user/check")
                .permitAll()
                .requestMatchers("/user-company/**").hasAnyRole(SADMIN.name(),ADMIN.name(),EMPLOYEE.name(),PROSPECT.name(),RECRUITER.name(),SALES.name(),HR_MANAGER.name(),"GROUP_ADMIN")
               .requestMatchers("/employees/**","/orders/**").hasAnyRole(SADMIN.name(),ADMIN.name(),EMPLOYEE.name(), PROSPECT.name(),RECRUITER.name(),SALES.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers("/candidates/**").hasAnyRole(SADMIN.name(),ADMIN.name(),RECRUITER.name(), SALES.name())
                .requestMatchers(GET,"/candidates/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),RECRUITER.name(), SALES.name())
                .requestMatchers(POST,"/candidates/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),RECRUITER.name(), SALES.name())
                .requestMatchers(PUT,"/candidates/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),RECRUITER.name(), SALES.name())
                .requestMatchers(DELETE,"/candidates/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),RECRUITER.name(), SALES.name())
                .requestMatchers("/timesheets/**","/companies/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers(GET,"/timesheets/**","/companies/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),ADMIN_READ.name(),"ROLE_GROUP_ADMIN")
                .requestMatchers(POST,"/timesheets/**","/companies/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),ADMIN_CREATE.name(),"ROLE_GROUP_ADMIN")
                .requestMatchers(PUT,"/timesheets/**","/companies/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),ADMIN_UPDATE.name(),"ROLE_GROUP_ADMIN")
                .requestMatchers(DELETE,"/timesheets/**","/companies/**").hasAnyAuthority(SADMIN.name(),ADMIN.name(),ADMIN_DELETE.name(),"ROLE_GROUP_ADMIN")
                .requestMatchers("/email/***", "/contacts/**","/bulkmails/**").hasAnyRole(SADMIN.name(),ADMIN.name(), RECRUITER.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers("/address-verification/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),EMPLOYEE.name(),"GROUP_ADMIN")
                .requestMatchers("/timesheets/reminders/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers(POST,"/paystubs/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers(DELETE,"/paystubs/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers(GET,"/paystubs/all").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN")
                .requestMatchers(GET,"/paystubs/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN",EMPLOYEE.name())
                .requestMatchers("/paystubs/**").hasAnyRole(SADMIN.name(),ADMIN.name(),HR_MANAGER.name(),"GROUP_ADMIN",EMPLOYEE.name())
                .requestMatchers(GET,"/employees/**","/orders/**","/trackings/**","/project-history/**","/visa-details/**").hasAnyAuthority(SADMIN.name(),ADMIN_READ.name(),EMPLOYEE_READ.name())
                .requestMatchers(POST,"/employees/**","/orders/**","/trackings/**","/project-history/**","/visa-details/**","/auth/register","/auth/resetPassword","/auth/updatePassword").hasAnyAuthority(SADMIN_CREATE.name(),ADMIN_CREATE.name(),EMPLOYEE_CREATE.name(),RECRUITER.name(), SALES.name())
                .requestMatchers(PUT,"/employees/**","/orders/**","/trackings/**","/project-history/**","/visa-details/**").hasAnyAuthority(SADMIN_UPDATE.name(),ADMIN_UPDATE.name(),EMPLOYEE_UPDATE.name(),PROSPECT_UPDATE.name())
                .requestMatchers(DELETE,"/employees/**","/orders/**","/trackings/**","/project-history/**","/visa-details/**").hasAnyAuthority(SADMIN_DELETE.name(),ADMIN_DELETE.name(),EMPLOYEE_DELETE.name())
                .anyRequest()
                .authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
