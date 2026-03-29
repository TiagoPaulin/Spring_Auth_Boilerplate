package com.boilerplate.springauth.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    UserDetails findByEmail (String email);

    boolean existsByEmail (String email);

    long countByRole (Role role);

    Page<User> findAll(Pageable pageable);
    
}
