package pl.bpiatek.linkshorteneruserservice.api.dto;

public record LoginResponse(String accessToken, String refreshToken) {
}
