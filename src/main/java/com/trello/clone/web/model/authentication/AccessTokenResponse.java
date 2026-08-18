package com.trello.clone.web.model.authentication;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "A freshly issued access token")
public class AccessTokenResponse {

    @Schema(description = "Short-lived JWT used to authorize API requests")
    private String accessToken;

    public AccessTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

}
