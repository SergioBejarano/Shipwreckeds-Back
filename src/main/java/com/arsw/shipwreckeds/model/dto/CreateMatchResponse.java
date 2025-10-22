package com.arsw.shipwreckeds.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * DTO respuesta al crear partida (contiene código).
 * @author Daniel
 * @version 22/10/2025
 */
@Getter
@AllArgsConstructor
public class CreateMatchResponse {
    private String code;
}
