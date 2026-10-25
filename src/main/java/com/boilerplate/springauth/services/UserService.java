package com.boilerplate.springauth.services;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.boilerplate.springauth.dto.user.RoleUpdateRequest;
import com.boilerplate.springauth.dto.user.UserCreateRequest;
import com.boilerplate.springauth.dto.user.UserResponse;
import com.boilerplate.springauth.dto.user.UserUpdateRequest;
import com.boilerplate.springauth.exceptions.BadRequestException;
import com.boilerplate.springauth.exceptions.ResourceNotFoundException;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.models.enums.Role;
import com.boilerplate.springauth.repositories.ResetPasswordTokenRepository;
import com.boilerplate.springauth.repositories.UserRepository;
import com.boilerplate.springauth.security.RefreshTokenService;

import jakarta.transaction.Transactional;

@Service
public class UserService {

    private final UserRepository repository;
    private final RefreshTokenService refreshTokenService;
    private final ResetPasswordTokenRepository resetPasswordTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, RefreshTokenService refreshTokenService, ResetPasswordTokenRepository resetPasswordTokenRepository, PasswordEncoder passwordEncoder) {

        this.repository = repository;
        this.refreshTokenService = refreshTokenService;
        this.resetPasswordTokenRepository = resetPasswordTokenRepository;
        this.passwordEncoder = passwordEncoder;

    }

    @Transactional
    public Page<UserResponse> findAll (Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional
    public UserResponse create (UserCreateRequest request) {

       validateCreationBusinessRules(request);

        User user = toUser(request);
        user = repository.save(user);

        return toResponse(user);

    }

    public UserResponse findUser (Long id) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return toResponse(user);

    }

    @Transactional
    public void delete (Long id) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isActiveAdmin(user)) {
            validateIsNotLastAdmin();
        }

        refreshTokenService.revokeAll(user);
        resetPasswordTokenRepository.deleteByUser(user);
        repository.deleteById(id);

    }

    @Transactional
    public UserResponse update (Long id, UserUpdateRequest updateRequest) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        validateUpdateBusinessRules(updateRequest, user);

        boolean securitySensitiveChange = false;

        if (updateRequest.name() != null) user.setName(updateRequest.name());
        if (updateRequest.email() != null && !updateRequest.email().equals(user.getEmail())) {
            user.setEmail(updateRequest.email());
            securitySensitiveChange = true;
        }
        if (updateRequest.password() != null) {
            user.setPassword(passwordEncoder.encode(updateRequest.password()));
            securitySensitiveChange = true;
        }

        if (securitySensitiveChange) {
            refreshTokenService.revokeAll(user);
        }

        return toResponse(repository.save(user));

    }

    @Transactional
    public void activate (Long id) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setActive(true);

        repository.save(user);

    }

    @Transactional
    public void deactivate (Long id) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isActiveAdmin(user)) {
            validateIsNotLastAdmin();
        }

        user.setActive(false);

        repository.save(user);
        refreshTokenService.revokeAll(user);

    }

    @Transactional
    public void changeRole (Long id, RoleUpdateRequest roleRequest) {

        User user = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (isActiveAdmin(user) && roleRequest.role() != Role.ADMIN) {
            validateIsNotLastAdmin();
        }

        user.setRole(roleRequest.role());

        repository.save(user);
        refreshTokenService.revokeAll(user);

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
            user.getCreatedAt(),
            user.getUpdatedAt()
        );

    }

    private void validateCreationBusinessRules (UserCreateRequest request) {

        if (repository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already in use");
        }

    }

    private void validateUpdateBusinessRules (UserUpdateRequest request, User user) {

        if (request.email() != null && repository.existsByEmail(request.email()) && !user.getEmail().equals(request.email())) {
            throw new BadRequestException("Email already in use");
        }

    }

    private boolean isActiveAdmin (User user) {

        return user.getRole() == Role.ADMIN && user.isActive();

    }

    private void validateIsNotLastAdmin () {

        if (repository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BadRequestException("The system must have at least one active administrator.");
        }

    }

}
