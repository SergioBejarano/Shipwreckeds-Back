package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Payload used by clients to respond to a match creation request.
 *
 */
@Getter
@AllArgsConstructor
public class CreateMatchResponse {
    private String code;
}
