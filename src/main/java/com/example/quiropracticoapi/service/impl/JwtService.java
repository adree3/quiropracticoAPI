package com.example.quiropracticoapi.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (userDetails instanceof com.example.quiropracticoapi.model.Usuario user) {
            if (user.getClinica() != null) {
                extraClaims.put("clinicaId", user.getClinica().getIdClinica());
            }
        }
        return generateToken(extraClaims, userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        String subject = userDetails.getUsername();
        if (userDetails instanceof com.example.quiropracticoapi.model.Usuario user) {
            subject = "ID|" + user.getIdUsuario();
        }

        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String subject = extractUsername(token);
        
        boolean isValidUser;
        if (subject != null && subject.startsWith("ID|")) {
            try {
                Integer idFromToken = Integer.parseInt(subject.substring(3));
                if (userDetails instanceof com.example.quiropracticoapi.model.Usuario user) {
                    isValidUser = idFromToken.equals(user.getIdUsuario());
                } else {
                    isValidUser = false;
                }
            } catch (NumberFormatException e) {
                isValidUser = false;
            }
        } else {
            isValidUser = (subject != null && subject.equals(userDetails.getUsername()));
        }
        
        return isValidUser && !isTokenExpired(token);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public Long extractClinicaId(String token) {
        return extractClaim(token, claims -> {
            Object val = claims.get("clinicaId");
            if (val instanceof Number) return ((Number) val).longValue();
            return null;
        });
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private SecretKey getSignInKey() {
        // 3. Usamos la variable inyectada 'secretKey'
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
