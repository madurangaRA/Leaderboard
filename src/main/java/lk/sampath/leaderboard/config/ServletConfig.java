package lk.sampath.leaderboard.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ClassUtils;

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
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setName("errorPageFilterRegistration");
        registration.setEnabled(false);

        if (!ClassUtils.isPresent("org.springframework.boot.web.servlet.error.ErrorPageFilter", getClass().getClassLoader())) {
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
            registration.setFilter(noopFilter);
            return registration;
        }
        try {
            Class<?> clazz = Class.forName("org.springframework.boot.web.servlet.error.ErrorPageFilter");
            Object filterInstance = clazz.getDeclaredConstructor().newInstance();
            registration.setFilter((Filter) filterInstance);
            return registration;
        } catch (Exception ex) {
            // if creation fails, return disabled registration with noop filter
            Filter noopFilter = new Filter() {
                @Override
                public void init(FilterConfig filterConfig) {
                }

                @Override
                public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
                    chain.doFilter(request, response);
                }

                @Override
                public void destroy() {
                }
            };
            registration.setFilter(noopFilter);
            return registration;
        }
    }
}
