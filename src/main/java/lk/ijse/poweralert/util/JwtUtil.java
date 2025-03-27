package lk.ijse.poweralert.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lk.ijse.poweralert.dto.UserDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Token valid for 12 days
    public static final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 12;

    // Use a default secret key if none provided in properties
    @Value("${jwt.secret:powerAlertSecureSecretKeyWithAtLeast256BitsLength}")
    private String secretKeyString;

    private Key key;

    @PostConstruct
    public void init() {
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[64]; // 512 bits
        random.nextBytes(keyBytes);
        this.key = Keys.hmacShaKeyFor(keyBytes);
        logger.info("JWT signing key initialized with new random key");
    }

    // Retrieve username from JWT token
    public String extractUsername(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Retrieve expiration date from JWT token
    public Date extractExpiration(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // For retrieving any information from token we will need the secret key
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Check if the token has expired
    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            logger.error("Error checking token expiration: {}", e.getMessage());
            return true; // Consider expired if there's an error
        }
    }

    // Generate token for user
    public String generateToken(UserDTO userDTO) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDTO.getRole().name()); // Store role name as string
        claims.put("userId", userDTO.getId());
        claims.put("email", userDTO.getEmail());

        String token = doGenerateToken(claims, userDTO.getUsername());
        logger.debug("Generated token for user {} with role {}", userDTO.getUsername(), userDTO.getRole().name());

        return token;
    }

    // Create the token
    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(key)
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            logger.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }
}