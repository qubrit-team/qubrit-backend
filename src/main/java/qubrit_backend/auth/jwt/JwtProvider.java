package qubrit_backend.auth.jwt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

	private final SecretKey key;
	private final long expirationMs;

	public JwtProvider(@Value("${jwt.secret}") String secret,
					   @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
		this.expirationMs = expirationMs;
	}

	public String createToken(Long userId, String email) {
		Date now = new Date();
		return Jwts.builder()
				.subject(String.valueOf(userId))
				.claim("email", email)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	// 유효하면 userId, 아니면 null
	public Long parseUserId(String token) {
		try {
			String sub = Jwts.parser().verifyWith(key).build()
					.parseSignedClaims(token).getPayload().getSubject();
			return Long.valueOf(sub);
		} catch (JwtException | IllegalArgumentException e) {
			return null;
		}
	}
}
