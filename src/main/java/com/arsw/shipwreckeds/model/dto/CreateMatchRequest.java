package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Payload used by clients to request the creation of a new match.
 *
 */
@Getter
@Setter
public class CreateMatchRequest {
    private String hostName;
}
