package com.planner.app.auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.planner.app.auth.api.dto.LoginDTO;
import com.planner.app.auth.api.dto.LoginResponseDTO;
import com.planner.app.auth.api.dto.RegisterRequest;
import com.planner.app.auth.jwt.JwtAuthenticationFilter;
import com.planner.app.auth.jwt.JwtUtil;
import com.planner.app.config.SecurityConfig;
import com.planner.app.dto.UserDTO;
import com.planner.app.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link AuthController}. Boots only the web layer plus the
 * real {@link SecurityConfig} so the public-route rules are exercised; no
 * database is started.
 *
 * <p>{@code SecurityConfig} depends on {@link JwtAuthenticationFilter}, which in
 * turn needs {@link JwtUtil} and a {@link UserDetailsService}. Those are mocked
 * here so the filter chain wires up without the application context.
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // Collaborators that SecurityConfig / JwtAuthenticationFilter require in the slice.
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void signin_isPublicAndReturnsOkWithBody() throws Exception {
        LoginDTO request = new LoginDTO("ada", "secret");
        LoginResponseDTO response = LoginResponseDTO.builder()
                .token("signed.jwt.token")
                .message("Login successful")
                .build();
        when(authService.login(any(LoginDTO.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("signed.jwt.token"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void signup_isPublicAndReturnsCreatedOnSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ada");
        request.setMail("ada@example.com");
        request.setPassword("secret");
        UserDTO created = new UserDTO();
        created.setUsername("ada");
        created.setMail("ada@example.com");
        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("ada"));
    }

    @Test
    void signup_returnsBadRequestWithMeaningfulBodyWhenServiceThrows() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ada");
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                // The body must carry the failure reason, not be empty.
                .andExpect(content().string("Username already exists"));
    }
}
