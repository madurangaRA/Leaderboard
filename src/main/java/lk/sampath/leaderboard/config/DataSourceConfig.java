package lk.sampath.leaderboard.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    private final DataSourceProperties dataSourceProperties;

    public DataSourceConfig(DataSourceProperties dataSourceProperties) {
        this.dataSourceProperties = dataSourceProperties;
    }

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    @SuppressWarnings({"unchecked","rawtypes"})
    public javax.sql.DataSource dataSource() {
        // If no URL is configured via active profile, try common environment/system variables
        String configuredUrl = this.dataSourceProperties.getUrl();
        if (configuredUrl == null || configuredUrl.isBlank()) {
            log.warn("No spring.datasource.url configured via active profile. Checking environment variables and system properties...");
            String[] candidates = new String[] {
                    System.getProperty("spring.datasource.url"),
                    System.getProperty("SPRING_DATASOURCE_URL"),
                    System.getenv("SPRING_DATASOURCE_URL"),
                    System.getenv("JDBC_URL"),
                    System.getenv("DATABASE_URL")
            };
            for (String c : candidates) {
                if (c != null && !c.isBlank()) {
                    log.info("Detected database URL from environment/system property.");
                    this.dataSourceProperties.setUrl(c);
                    configuredUrl = c;
                    break;
                }
            }
        }

        if (configuredUrl == null || configuredUrl.isBlank()) {
            String msg = "No datasource URL found. Set 'spring.profiles.active' to 'local' or 'test' in Tomcat (e.g. CATALINA_OPTS='-Dspring.profiles.active=local') or provide SPRING_DATASOURCE_URL/JDBC_URL env var.";
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        // If driver not set, attempt to infer for common DBs
        String driver = this.dataSourceProperties.getDriverClassName();
        if (driver == null || driver.isBlank()) {
            if (configuredUrl.contains("mysql")) {
                log.info("No driver-class-name configured; assuming MySQL driver 'com.mysql.cj.jdbc.Driver'");
                this.dataSourceProperties.setDriverClassName("com.mysql.cj.jdbc.Driver");
            }
        }

        try {
            Class<?> tomcatDs = Class.forName("org.apache.tomcat.jdbc.pool.DataSource");
            return this.dataSourceProperties
                    .initializeDataSourceBuilder()
                    .type((Class) tomcatDs)
                    .build();
        } catch (ClassNotFoundException e) {
            log.info("Tomcat JDBC pool not present on application classpath; using default DataSource builder.");
            return this.dataSourceProperties.initializeDataSourceBuilder().build();
        }
    }
}
