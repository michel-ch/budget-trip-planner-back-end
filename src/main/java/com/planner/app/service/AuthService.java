package com.planner.app.service;

import com.planner.app.auth.api.dto.LoginDTO;
import com.planner.app.auth.api.dto.LoginResponseDTO;
import com.planner.app.auth.api.dto.RegisterRequest;
import com.planner.app.auth.jwt.JwtUtil;
import com.planner.app.dao.LocationRepository;
import com.planner.app.dao.UserRepository;
import com.planner.app.dto.UserDTO;
import com.planner.app.entity.Location;
import com.planner.app.entity.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public LoginResponseDTO login(LoginDTO request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            String token = jwtUtil.generateToken(request.getUsername());

            UserDTO userDTO = userRepository.findByUsernameOrEmailDTO(request.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found after authentication"));

            return LoginResponseDTO.builder()
                    .token(token)
                    .user(userDTO)
                    .message("Login successful")
                    .build();
        } catch (BadCredentialsException e) {
            log.error("Invalid credentials for user: {}", request.getUsername());
            throw new RuntimeException("Invalid username or password");
        }
    }

    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByMail(request.getMail())) {
            throw new RuntimeException("Email already exists");
        }

        Location location = new Location();
        location.setCity(request.getCity());
        location.setCountry(request.getCountry());
        locationRepository.save(location);

        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setMail(request.getMail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // Hash password
        user.setBirthday(request.getBirthday());
        user.setLocation_id(location);

        User savedUser = userRepository.save(user);

        return convertToDTO(savedUser);
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setUsername(user.getUsername());
        dto.setMail(user.getMail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setBirthday(user.getBirthday());
        dto.setLocation(user.getLocation_id());
        return dto;
    }

}
