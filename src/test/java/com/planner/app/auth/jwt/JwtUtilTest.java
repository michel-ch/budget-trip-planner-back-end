package com.planner.app.auth.jwt;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pure unit tests for {@link JwtUtil}. No Spring context and no database.
 *
 * <p>{@code JwtUtil} reads {@code secret} and {@code expiration} via field
 * injection ({@code @Value}), so the test seeds those fields with
 * {@link ReflectionTestUtils} instead of booting a context.
 */
class JwtUtilTest {

    private static final String TEST_SECRET = "test-secret-do-not-use-in-prod";
    private static final long ONE_DAY_MS = 86_400_000L;
    private static final String ISSUER = "planner-app";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", ONE_DAY_MS);
    }

    @Test
    void generateToken_producesNonEmptyToken() {
        String token = jwtUtil.generateToken("alice");

        assertThat(token).isNotBlank();
        // A signed JWT is three base64url segments joined by dots.
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void getUsernameFromToken_roundTripsTheSubject() {
        String token = jwtUtil.generateToken("alice");

        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo("alice");
    }

    @Test
    void generateToken_setsPlannerAppIssuer() {
        String token = jwtUtil.generateToken("alice");

        DecodedJWT decoded = JWT.decode(token);
        assertThat(decoded.getIssuer()).isEqualTo(ISSUER);
        assertThat(decoded.getSubject()).isEqualTo("alice");
    }

    @Test
    void validateToken_acceptsFreshlyGeneratedToken() {
        String token = jwtUtil.generateToken("alice");

        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_rejectsGarbageToken() {
        assertThat(jwtUtil.validateToken("not-a-real-jwt")).isFalse();
    }

    @Test
    void validateToken_rejectsTamperedToken() {
        String token = jwtUtil.generateToken("alice");
        String[] segments = token.split("\\.");
        // Re-encode the payload with a different subject but keep the original
        // signature, so the HMAC over (header.payload) no longer matches.
        String forgedPayload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"mallory\",\"iss\":\"planner-app\"}"
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        String tampered = segments[0] + "." + forgedPayload + "." + segments[2];

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_rejectsTokenSignedWithDifferentSecret() {
        // Token correctly shaped (right issuer, valid structure) but signed with
        // another key, so the HMAC check must fail.
        String foreignToken = JWT.create()
                .withSubject("alice")
                .withIssuer(ISSUER)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + ONE_DAY_MS))
                .sign(Algorithm.HMAC256("a-completely-different-secret"));

        assertThat(jwtUtil.validateToken(foreignToken)).isFalse();
        assertThat(jwtUtil.getUsernameFromToken(foreignToken)).isNull();
    }

    @Test
    void validateToken_rejectsTokenWithWrongIssuer() {
        String wrongIssuerToken = JWT.create()
                .withSubject("alice")
                .withIssuer("someone-else")
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + ONE_DAY_MS))
                .sign(Algorithm.HMAC256(TEST_SECRET));

        assertThat(jwtUtil.validateToken(wrongIssuerToken)).isFalse();
    }
}
