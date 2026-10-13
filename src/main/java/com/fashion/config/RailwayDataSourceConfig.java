package com.fashion.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/** Turns Railway's DATABASE_URL into the JDBC connection Spring actually uses. */
@Configuration
public class RailwayDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(RailwayDataSourceConfig.class);

    @Bean
    public DataSource dataSource(Environment environment) {
        String jdbcUrl;
        String username;
        String password;

        String raw = environment.getProperty("DATABASE_URL");
        if (raw == null || raw.isBlank()) {
            jdbcUrl = environment.getProperty("spring.datasource.url");
            username = environment.getProperty("spring.datasource.username");
            password = environment.getProperty("spring.datasource.password");
            log.info("DATABASE_URL is not set. Using {}", hostOf(jdbcUrl));
        } else {
            ParsedUrl parsed = parse(raw.replaceAll("\\s+", ""));
            jdbcUrl = parsed.jdbcUrl;
            username = parsed.username != null ? parsed.username : environment.getProperty("spring.datasource.username");
            password = parsed.password != null ? parsed.password : environment.getProperty("spring.datasource.password");
            log.info("DATABASE_URL is set. Using {}", hostOf(jdbcUrl));
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        String driver = environment.getProperty("spring.datasource.driver-class-name");
        if (driver != null && !driver.isBlank()) {
            config.setDriverClassName(driver);
        }
        if (username != null) {
            config.setUsername(username);
        }
        if (password != null) {
            config.setPassword(password);
        }
        config.setMaximumPoolSize(5);
        config.setConnectionTimeout(60_000);
        config.setInitializationFailTimeout(60_000);
        return new HikariDataSource(config);
    }

    private static ParsedUrl parse(String raw) {
        if (raw.startsWith("jdbc:")) {
            return new ParsedUrl(raw, null, null);
        }
        String normalized = raw.startsWith("postgres://")
                ? "postgresql://" + raw.substring("postgres://".length())
                : raw;
        if (!normalized.startsWith("postgresql://")) {
            throw new IllegalStateException("DATABASE_URL must start with postgresql://");
        }

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("DATABASE_URL is not a valid postgres URL");
        }
        if (uri.getHost() == null) {
            throw new IllegalStateException("DATABASE_URL is missing a host");
        }

        String username = null;
        String password = null;
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            int colon = userInfo.indexOf(':');
            if (colon >= 0) {
                username = decode(userInfo.substring(0, colon));
                password = decode(userInfo.substring(colon + 1));
            } else {
                username = decode(userInfo);
            }
        }

        String database = uri.getPath() == null ? "" : uri.getPath();
        if (database.startsWith("/")) {
            database = database.substring(1);
        }
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://")
                .append(uri.getHost())
                .append(':')
                .append(port)
                .append('/')
                .append(database);
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            jdbc.append('?').append(uri.getQuery());
        }
        return new ParsedUrl(jdbc.toString(), username, password);
    }

    private static String hostOf(String jdbcUrl) {
        if (jdbcUrl == null) {
            return "no database url";
        }
        String withoutPrefix = jdbcUrl.startsWith("jdbc:") ? jdbcUrl.substring(5) : jdbcUrl;
        try {
            URI uri = URI.create(withoutPrefix);
            if (uri.getHost() == null) {
                return withoutPrefix;
            }
            return uri.getHost() + ":" + (uri.getPort() == -1 ? 5432 : uri.getPort());
        } catch (IllegalArgumentException ex) {
            return "unreadable database url";
        }
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private record ParsedUrl(String jdbcUrl, String username, String password) {
    }
}
