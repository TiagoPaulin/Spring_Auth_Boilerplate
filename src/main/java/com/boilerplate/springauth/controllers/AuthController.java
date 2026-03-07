package com.boilerplate.springauth.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.boilerplate.springauth.dto.login.LoginRequest;
import com.boilerplate.springauth.dto.login.LoginResponse;
import com.boilerplate.springauth.models.User;
import com.boilerplate.springauth.security.TokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping(value = "/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

    public AuthController (AuthenticationManager authenticationManager, TokenService tokenService) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest request) {

        var usernamePassword = new UsernamePasswordAuthenticationToken(request.email(), request.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());

        return ResponseEntity.ok(new LoginResponse(token));

    }

}
