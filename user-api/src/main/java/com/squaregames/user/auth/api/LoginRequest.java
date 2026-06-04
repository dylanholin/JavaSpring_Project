package com.squaregames.user.auth.api;

public record LoginRequest(
    String email,
    String password
) {}
