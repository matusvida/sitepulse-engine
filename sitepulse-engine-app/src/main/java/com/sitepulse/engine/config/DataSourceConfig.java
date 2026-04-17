package com.sitepulse.engine.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource dataSource(SitePulseProperties properties) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(toJdbcUrl(properties.postgresDsn()));
        dataSource.setUsername(extractUsername(properties.postgresDsn()));
        dataSource.setPassword(extractPassword(properties.postgresDsn()));
        return dataSource;
    }

    private String toJdbcUrl(String dsn) {
        if (dsn.startsWith("jdbc:")) {
            return dsn;
        }
        if (!dsn.startsWith("postgresql://")) {
            throw new IllegalStateException("Unsupported Postgres DSN: " + dsn);
        }
        String noScheme = dsn.substring("postgresql://".length());
        int atIndex = noScheme.indexOf('@');
        String hostAndDb = atIndex >= 0 ? noScheme.substring(atIndex + 1) : noScheme;
        return "jdbc:postgresql://" + hostAndDb;
    }

    private String extractUsername(String dsn) {
        String credentials = extractCredentials(dsn);
        int colonIndex = credentials.indexOf(':');
        return colonIndex >= 0 ? credentials.substring(0, colonIndex) : credentials;
    }

    private String extractPassword(String dsn) {
        String credentials = extractCredentials(dsn);
        int colonIndex = credentials.indexOf(':');
        return colonIndex >= 0 ? credentials.substring(colonIndex + 1) : "";
    }

    private String extractCredentials(String dsn) {
        if (!dsn.startsWith("postgresql://")) {
            return "";
        }
        String noScheme = dsn.substring("postgresql://".length());
        int atIndex = noScheme.indexOf('@');
        return atIndex >= 0 ? noScheme.substring(0, atIndex) : "";
    }
}
