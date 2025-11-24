package lk.sampath.leaderboard.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

@Configuration
public class ServletConfig {

    @Bean
    public FilterRegistrationBean<Filter> disableErrorPageFilterRegistration() {
        // Register a noop filter and disable the registration so the container does not try to use
        // Spring Boot's ErrorPageFilter. This avoids classpath/reflection issues and guarantees
        // the FilterRegistrationBean.getFilter() is never null during startup.
        Filter noopFilter = new Filter() {
            @Override
            public void init(FilterConfig filterConfig) {
                // no-op
            }

            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                chain.doFilter(request, response);
            }

            @Override
            public void destroy() {
                // no-op
            }
        };

        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(noopFilter);
        // Do not set the servlet registration name to avoid colliding with an existing
        // 'errorPageFilterRegistration' entry. Keep the registration disabled so no
        // runtime registration is performed by default.
        // Set an explicit unique name to prevent accidental collisions with other
        // FilterRegistrationBean instances that may try to register the same default name.
        registration.setName("noopErrorPageFilterRegistration");
        registration.setEnabled(false);
        return registration;
    }
}
