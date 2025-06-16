package com.daacooerp.erp.config;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import javax.crypto.SecretKey;

@Component
public class JwtConfig {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private long expiration;
    
    // 存储生成的密钥，确保每次使用相同的密钥
    private SecretKey secretKey;
    
    private SecretKey getSigningKey() {
        // 如果密钥已经生成，直接返回
        if (secretKey != null) {
            return secretKey;
        }
        
        // 根据RFC 7518规范，HMAC-SHA算法的密钥必须至少为256位
        // 使用配置的secret作为种子，生成一个安全的密钥
        // 使用推荐的方法创建安全的密钥
        secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        
        // 如果密钥长度不足，则使用secretKeyFor方法生成一个新的安全密钥
        if (secretKey.getEncoded().length * 8 < 256) {
            secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }
        
        return secretKey;
    }
    
    public String generateToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }
    
    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }
    
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}