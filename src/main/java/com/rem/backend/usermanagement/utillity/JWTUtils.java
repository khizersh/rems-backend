package com.rem.backend.usermanagement.utillity;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;


@Component
public class JWTUtils {

    private final String SECRET_KEY = "your-256-bit-long-secret-key-which-has-enough-length-for-HS256";
    private final int EXPIRY_HOURS = 10;


    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
//                .setExpiration(new Date(System.currentTimeMillis() + 10 * 1000 * 60 * 60)) // 1 hour expiry
                .setExpiration(new Date(System.currentTimeMillis() +  Duration.ofHours(EXPIRY_HOURS).toMillis())) // 1 hour expiry
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes());
    }

    public String extractUsername(String token) {
        return Jwts.parser() // Use parser() if parserBuilder() isn't found
                .setSigningKey(getSigningKey()) // New method in JJWT 0.12.6
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token, String username) {
        return username.equals(extractUsername(token)) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parser()
                .setSigningKey(getSigningKey())
                .build()
                .parseSignedClaims(token)  // ✅ Correct method in JJWT 0.12.6
                .getPayload()
                .getExpiration()
                .before(new Date());
    }
}
