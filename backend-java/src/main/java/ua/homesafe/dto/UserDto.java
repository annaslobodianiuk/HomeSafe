package ua.homesafe.dto;

import ua.homesafe.model.UserEntity;

import java.time.OffsetDateTime;

public record UserDto(String id, String name, String email, String role, String status, OffsetDateTime createdAt) {

    public static UserDto from(UserEntity user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getStatus(), user.getCreatedAt());
    }
}
