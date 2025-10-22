package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO para petición de creación de partida.
 * @author Daniel
 * @version 22/10/2025
 */
@Getter
@Setter
public class CreateMatchRequest {
    private String hostName;
}
