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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Mockito unit tests for {@link AuthService}. All collaborators are mocked, so
 * no Spring context and no database are required. Assertions focus on observable
 * outputs and interactions, not private internals.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest sampleRegisterRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("Ada");
        request.setLastName("Lovelace");
        request.setUsername("ada");
        request.setMail("ada@example.com");
        request.setPassword("plaintext-password");
        request.setCity("London");
        request.setCountry("UK");
        return request;
    }

    @Test
    void register_savesLocationThenUserWithEncodedPasswordAndReturnsDto() {
        RegisterRequest request = sampleRegisterRequest();
        when(userRepository.existsByUsername("ada")).thenReturn(false);
        when(userRepository.existsByMail("ada@example.com")).thenReturn(false);
        when(passwordEncoder.encode("plaintext-password")).thenReturn("ENCODED-HASH");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDTO result = authService.register(request);

        // A Location is persisted before the User.
        verify(locationRepository).save(any(Location.class));

        // The persisted User carries the BCrypt-encoded password, never the plaintext.
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertThat(saved.getPassword()).isEqualTo("ENCODED-HASH");
        assertThat(saved.getPassword()).isNotEqualTo("plaintext-password");
        assertThat(saved.getUsername()).isEqualTo("ada");
        assertThat(saved.getMail()).isEqualTo("ada@example.com");

        // The returned DTO mirrors the registered user.
        assertThat(result.getUsername()).isEqualTo("ada");
        assertThat(result.getMail()).isEqualTo("ada@example.com");
        assertThat(result.getFirstName()).isEqualTo("Ada");
        assertThat(result.getLastName()).isEqualTo("Lovelace");
    }

    @Test
    void register_throwsWhenUsernameAlreadyExists() {
        RegisterRequest request = sampleRegisterRequest();
        when(userRepository.existsByUsername("ada")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username already exists");

        verify(locationRepository, never()).save(any(Location.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        RegisterRequest request = sampleRegisterRequest();
        when(userRepository.existsByUsername("ada")).thenReturn(false);
        when(userRepository.existsByMail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Email already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_returnsTokenAndUserOnSuccess() {
        LoginDTO request = new LoginDTO("ada", "plaintext-password");
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtil.generateToken("ada")).thenReturn("signed.jwt.token");
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("ada");
        when(userRepository.findByUsernameOrEmailDTO("ada")).thenReturn(Optional.of(userDTO));

        LoginResponseDTO response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("signed.jwt.token");
        assertThat(response.getUser()).isSameAs(userDTO);
        assertThat(response.getMessage()).isEqualTo("Login successful");
    }

    @Test
    void login_throwsRuntimeExceptionOnBadCredentials() {
        LoginDTO request = new LoginDTO("ada", "wrong-password");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad creds"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid username or password");

        verify(jwtUtil, never()).generateToken(any());
    }
}
