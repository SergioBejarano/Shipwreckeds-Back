package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload used to exchange a Cognito authorization code for a session.
 */
@Getter
@Setter
public class LoginCodeRequest {
    private String code;
    private String redirectUri;
}
