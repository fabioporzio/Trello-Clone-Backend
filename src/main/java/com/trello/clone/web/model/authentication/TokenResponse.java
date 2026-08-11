package com.trello.clone.web.model.authentication;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Access and refresh tokens issued after a successful login")
public class TokenResponse {

    @Schema(description = "Short-lived JWT used to authorize API requests")
    private String accessToken;

    @Schema(description = "Long-lived token used to obtain a new access token")
    private String refreshToken;

    public TokenResponse(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
