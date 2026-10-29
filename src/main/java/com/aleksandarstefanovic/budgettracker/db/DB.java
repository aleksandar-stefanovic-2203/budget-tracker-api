package com.aleksandarstefanovic.budgettracker.db;

import javax.sql.DataSource;

import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration 
public class DB {
    private static DataSource dataSource;

    @Bean
    public static DataSource configureDataSource(Environment environment) {
        DriverManagerDataSource dmds = new DriverManagerDataSource();
        dmds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dmds.setUrl(environment.getRequiredProperty("spring.datasource.url"));
        dmds.setUsername(environment.getRequiredProperty("spring.datasource.username"));
        dmds.setPassword(environment.getRequiredProperty("spring.datasource.password"));
        dataSource = dmds;

        return dmds;
    }

    public static DataSource source() {
        if (dataSource == null) {
            throw new IllegalStateException("DataSource has not been configured by Spring.");
        }
        return dataSource;
    }
}
