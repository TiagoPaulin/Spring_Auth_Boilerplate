package com.boilerplate.springauth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.UserRepository;

@Configuration
public class InsertDatabaseConfig implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${api.admin.name}")
    private String nameAdmin;
    @Value("${api.admin.email}")
    private String emailAdmin;
    @Value("${api.admin.password}")
    private String passwordAdmin;

    public InsertDatabaseConfig (UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {

        if (!userRepository.existsByEmail(emailAdmin)) {
         
            User userAdmin = new User();

            userAdmin.setName(nameAdmin);
            userAdmin.setEmail(emailAdmin);
            userAdmin.setPassword(passwordEncoder.encode(passwordAdmin));
            userAdmin.setRole(Role.ADMIN);
            userAdmin.setActive(true);
            userRepository.save(userAdmin);

        }

    }

}
