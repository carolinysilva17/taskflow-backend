package com.carolinysilva.taskflow_backend.dto;

public record LoginResponse(String accessToken, UserResponse user) {
}
