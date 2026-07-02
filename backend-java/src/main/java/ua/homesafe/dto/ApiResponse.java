package ua.homesafe.dto;

public record ApiResponse<T>(T data, String message) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }
}
