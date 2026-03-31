package main;

import config.DataSourceProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

@SpringBootApplication
public class IngredientManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(IngredientManagementApplication.class, args);
    }

    @Bean
    public DataSourceProvider dataSourceProvider(DataSource dataSource) {
        return new DataSourceProvider(dataSource);
    }
}