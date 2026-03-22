package com.boilerplate.springauth.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.boilerplate.springauth.models.User;

@Service
public class AuthorizationService {
    
    public boolean isOwner(Long id) {

        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return user.getId().equals(id);

    }

}
