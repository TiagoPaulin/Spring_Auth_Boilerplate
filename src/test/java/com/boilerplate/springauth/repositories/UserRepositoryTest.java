package com.boilerplate.springauth.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;

@DataJpaTest
@ActiveProfiles("test")
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Should count only active users with the given role")
    void countByRoleAndActiveTrue_ShouldIgnoreInactiveUsers () {

        createAndPersistUser("active-admin@test.com", Role.ADMIN, true);
        createAndPersistUser("inactive-admin@test.com", Role.ADMIN, false);
        createAndPersistUser("active-user@test.com", Role.USER, true);

        assertThat(userRepository.countByRoleAndActiveTrue(Role.ADMIN)).isEqualTo(1);

    }

    private void createAndPersistUser (String email, Role role, boolean active) {

        User user = new User();

        user.setName("Tiago Teste");
        user.setEmail(email);
        user.setPassword("password123");
        user.setRole(role);
        user.setActive(active);

        entityManager.persist(user);

    }

}
