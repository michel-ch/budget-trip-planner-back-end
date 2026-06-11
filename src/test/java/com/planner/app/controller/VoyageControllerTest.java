package com.planner.app.controller;

import com.planner.app.auth.jwt.JwtAuthenticationFilter;
import com.planner.app.auth.jwt.JwtUtil;
import com.planner.app.config.SecurityConfig;
import com.planner.app.entity.Voyage;
import com.planner.app.service.VoyageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-slice tests for {@link VoyageController}'s protected route. Boots the web
 * layer plus the real {@link SecurityConfig}; no database is started.
 *
 * <p>Verifies the security contract from {@code SecurityConfig}: every non
 * {@code /api/auth/**} route requires authentication.
 */
@WebMvcTest(VoyageController.class)
@Import(SecurityConfig.class)
class VoyageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VoyageService voyageService;

    // Collaborators that SecurityConfig / JwtAuthenticationFilter require in the slice.
    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    void getVoyage_isRejectedWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/voyages/1"))
                // Unauthenticated access to a protected route is denied (401 or 403).
                .andExpect(status().is(anyOf401Or403()));
    }

    @Test
    @WithMockUser
    void getVoyage_returnsOkForAuthenticatedUser() throws Exception {
        Voyage voyage = new Voyage();
        voyage.setId(1);
        voyage.setDestination("Lisbon");
        when(voyageService.getVoyageById(eq(1))).thenReturn(voyage);

        mockMvc.perform(get("/api/voyages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.destination").value("Lisbon"));
    }

    private static org.hamcrest.Matcher<Integer> anyOf401Or403() {
        return org.hamcrest.Matchers.anyOf(
                org.hamcrest.Matchers.is(401),
                org.hamcrest.Matchers.is(403));
    }
}
