package com.boilerplate.springauth.services;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.boilerplate.springauth.repositories.UserRepository;

@Service
public class AuthService implements UserDetailsService {
    
    private final UserRepository repository;

    public AuthService (UserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return repository.findByEmail(username);
    }

}
