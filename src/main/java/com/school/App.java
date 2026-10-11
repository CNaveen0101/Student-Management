package com.school;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(considerNestedRepositories = true)
public class App {
    public static void main(String[] args) { SpringApplication.run(App.class, args); }

    // Creates a default login on first start. Change the password after signing in.
    @Bean
    CommandLineRunner seed(Models.UserRepo users) {
        return x -> {
            if (users.count() == 0) {
                Models.AppUser u = new Models.AppUser();
                u.username = "admin";
                u.passwordHash = Api.ENC.encode("admin123");
                users.save(u);
                System.out.println("Created default user: admin / admin123");
            }
        };
    }
}
