package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO para petición de unirse a partida.
 * @author Daniel
 * @version 22/10/2025
 */
@Getter
@Setter
public class JoinMatchRequest {
    private String code;
    private String username;
}
