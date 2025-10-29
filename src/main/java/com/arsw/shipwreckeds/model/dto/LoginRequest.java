package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload used by clients to request login.
 *
 */
@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
}
