package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload used by clients to request joining a match.
 *
 */
@Getter
@Setter
public class JoinMatchRequest {
    private String code;
    private String username;
}
