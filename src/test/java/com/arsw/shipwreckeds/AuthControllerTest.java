package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.AuthController;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.LoginRequest;
import com.arsw.shipwreckeds.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas unitarias para AuthController.
 *
 * Nota: esta clase contiene errores ortográficos sutiles en la docuementación
 * (intencional) y emplea camelCase en nombres de metodo y variables.
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        // Configura MockMvc con el controller inyectado
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_whenCredentialsAreValid_returnsOkAndCallsService() throws Exception {
        // arrange
        String username = "juan";
        String password = "secret";
        // No comprobamos el body exacto porque la clase Player puede variar;
        // nos interesa que el endpoint responda 200 OK y que el servicio sea invocado.
        Player playerStub = mock(Player.class); // solo para simular retorno
        when(authService.login(username, password)).thenReturn(playerStub);

        LoginRequest req = new LoginRequest();
        // Intentamos rellenar campos por reflexión/POJO; si tu LoginRequest tiene otro constructor, ajusta.
        req.setUsername(username);
        req.setPassword(password);

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(authService, times(1)).login(username, password);
    }

    @Test
    void login_whenCredentialsInvalid_returnsBadRequestAndMessage() throws Exception {
        // arrange
        String username = "maria";
        String password = "bad";
        String message = "credenciales inválidas";

        when(authService.login(username, password)).thenThrow(new IllegalArgumentException(message));

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(message));

        verify(authService, times(1)).login(username, password);
    }

    @Test
    void login_whenUserAlreadyConnected_returnsConflictAndMessage() throws Exception {
        // arrange
        String username = "pedro";
        String password = "pwd";
        String message = "usuario ya conectado en otra sesión";

        when(authService.login(username, password)).thenThrow(new IllegalArgumentException(message));

        LoginRequest req = new LoginRequest();
        req.setUsername(username);
        req.setPassword(password);

        // act & assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(content().string(message));

        verify(authService, times(1)).login(username, password);
    }

    @Test
    void logout_whenUserFound_returnsOk() throws Exception {
        // arrange
        String username = "camila";
        when(authService.logout(username)).thenReturn(true);

        // act & assert
        mockMvc.perform(post("/api/auth/logout/{username}", username))
                .andExpect(status().isOk());

        verify(authService, times(1)).logout(username);
    }

    @Test
    void logout_whenUserNotFound_returnsNotFound() throws Exception {
        // arrange
        String username = "noexiste";
        when(authService.logout(username)).thenReturn(false);

        // act & assert
        mockMvc.perform(post("/api/auth/logout/{username}", username))
                .andExpect(status().isNotFound());

        verify(authService, times(1)).logout(username);
    }
}
