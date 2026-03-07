package com.boilerplate.springauth.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Repository;

import com.boilerplate.springauth.models.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    UserDetails findByEmail (String email);

    boolean existsByEmail (String email);
    
}
