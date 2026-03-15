package com.boilerplate.springauth.dto.login;

public record LoginResponse(
    String token,
    String refreshToken
) {}