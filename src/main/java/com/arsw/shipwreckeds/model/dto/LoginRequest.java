package com.arsw.shipwreckeds.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO que representa los datos enviados desde el frontend
 * durante el inicio de sesión simple.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Getter
@Setter
public class LoginRequest {
    private String username;
    private String password;
}
