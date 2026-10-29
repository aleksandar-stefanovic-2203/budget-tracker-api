package com.aleksandarstefanovic.budgettracker.db;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration 
public class DB {
    @Bean
    public static DataSource source() {
        DriverManagerDataSource dmds = new DriverManagerDataSource();
        dmds.setDriverClassName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        dmds.setUrl("jdbc:sqlserver://localhost:1433;databaseName=budget_tracker;encrypt=false");
        dmds.setUsername("sa");
        dmds.setPassword("YourStrong!Passw0rd");

        return dmds;
    }
}
