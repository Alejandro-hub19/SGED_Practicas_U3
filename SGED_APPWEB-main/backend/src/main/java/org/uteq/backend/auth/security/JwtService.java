package org.uteq.backend.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${security.jwt.secret}")
    private String secret;

    @Value("${security.jwt.expiration-ms}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String username, String rol) {
        return buildToken(username, rol, expirationMs, "access");
    }

    public String generateRefreshToken(String username, String rol) {
        return buildToken(username, rol, refreshExpirationMs, "refresh");
    }

    /**
     * El JTI se emite con .id(), que es el claim estandar "jti" de RFC 7519.
     * Es el identificador unico que despues se usa como clave en la blacklist
     * de Redis. Sin un JTI unico por token, revocar uno revocaria todos los
     * tokens con la misma firma.
     */
    private String buildToken(String username, String rol, long expMs, String type) {
        return Jwts.builder()
                .subject(username)
                .id(UUID.randomUUID().toString())   // claim "jti" estandar
                .claim("rol", rol)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return extractClaims(token).getId();
    }

    public String extractRol(String token) {
        return extractClaims(token).get("rol", String.class);
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Milisegundos que le quedan de vida al token. Se usa como TTL de la
     * entrada en la blacklist: cuando el JWT expira por si mismo, la clave en
     * Redis ya no hace falta y se borra automaticamente.
     */
    public long getTiempoRestanteMs(String token) {
        long restante = extractClaims(token).getExpiration().getTime()
                - System.currentTimeMillis();
        return Math.max(restante, 0);
    }

    public boolean isTokenValid(String token) {
        try {
            return extractClaims(token).getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;   // firma invalida, malformado o expirado
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
