package com.planner.app.auth.api;

import com.planner.app.auth.api.dto.LoginDTO;
import com.planner.app.auth.api.dto.LoginResponseDTO;
import com.planner.app.auth.api.dto.RegisterRequest;
import com.planner.app.dto.UserDTO;
import com.planner.app.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<LoginResponseDTO> loginUser(@RequestBody LoginDTO request) {
        try {
            LoginResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(LoginResponseDTO.builder()
                            .message(e.getMessage())
                            .build());
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@RequestBody RegisterRequest request) {
        try {
            UserDTO user = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}
