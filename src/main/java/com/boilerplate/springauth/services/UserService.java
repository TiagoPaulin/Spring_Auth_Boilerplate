package com.boilerplate.springauth.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.boilerplate.springauth.dto.user.UserCreateRequest;
import com.boilerplate.springauth.dto.user.UserResponse;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.UserRepository;

@Service
public class UserService {
    
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.passwordEncoder = passwordEncoder;

    }

    public UserResponse create (UserCreateRequest request) {

       validateBusinessRules(request);

        User user = toUser(request);
        user = repository.save(user);

        return toResponse(user);

    }

    public UserResponse findUser (Long id) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toResponse(user);

    }

    private User toUser (UserCreateRequest request) {

        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);
        user.setActive(true);

        return user;

    }

    private UserResponse toResponse (User user) {

        return new UserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getCreatedAt()
        );

    }

    private void validateBusinessRules (UserCreateRequest request) {

        if (repository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already in use");
        }

    }

}
