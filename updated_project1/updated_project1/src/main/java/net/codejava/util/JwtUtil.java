package net.codejava.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;

@Component
public class JwtUtil {


    private static final String SECRET =
            System.getenv().getOrDefault("JWT_SECRET",
                    "mysecretkey12345678901234567890123456789012");


    private static final long EXPIRATION_MS = 5 * 60 * 1000L;


    public static final long RENEW_WINDOW_MS = 2 * 60 * 1000L;

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    /* -------- create -------- */
    public String generateToken(String username) {
        return generateToken(username, EXPIRATION_MS);
    }


    public String generateToken(String username, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .setId(jti)                 // JTI
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }


    public String extractUsername(String token) {
        return getClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return getClaim(token, Claims::getId);
    }

    public Date getExpirationDate(String token) {
        return getClaim(token, Claims::getExpiration);
    }

    public Date getIssuedAt(String token) {
        return getClaim(token, Claims::getIssuedAt);
    }

    public <T> T getClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }



    public long getRemainingMillis(String token) {
        return getExpirationDate(token).getTime() - System.currentTimeMillis();
    }


    public boolean isNearExpiry(String token) {
        long remaining = getRemainingMillis(token);
        return remaining > 0 && remaining <= RENEW_WINDOW_MS;
    }
}
