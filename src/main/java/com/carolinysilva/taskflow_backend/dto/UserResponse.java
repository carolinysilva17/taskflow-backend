package com.carolinysilva.taskflow_backend.dto;

import com.carolinysilva.taskflow_backend.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCreatedAt());
    }
}
