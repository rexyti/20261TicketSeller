package com.ticketseller.infrastructure.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class TestJwtFactory {

    public static final String TEST_SECRET = "test-secret-for-unit-tests-min-256-bits-here!!";

    public static String generarToken(UUID usuarioId, String rol) {
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("rol", rol)
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
