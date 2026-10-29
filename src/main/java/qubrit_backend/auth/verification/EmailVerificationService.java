package qubrit_backend.auth.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 이메일 인증코드 발급/검증. 상태는 메모리에만 보관하므로 서버 재시작 시 초기화된다.
@Service
public class EmailVerificationService {

	private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);
	private static final Duration CODE_TTL = Duration.ofMinutes(5);
	private static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
	private static final Duration VERIFIED_TTL = Duration.ofMinutes(30);
	private static final int MAX_ATTEMPTS = 5;

	private static class Entry {
		String code;
		Instant sentAt;
		Instant expiresAt;
		int attempts;
		boolean verified;
	}

	private final Map<String, Entry> entries = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();
	private final JavaMailSender mailSender;
	private final String from;

	public EmailVerificationService(JavaMailSender mailSender, @Value("${mailer.from}") String from) {
		this.mailSender = mailSender;
		this.from = from;
	}

	public void sendCode(String rawEmail) {
		String email = normalize(rawEmail);
		Instant now = Instant.now();
		Entry old = entries.get(email);
		if (old != null && now.isBefore(old.sentAt.plus(RESEND_INTERVAL))) {
			log.warn("인증코드 재발송 제한: {}", email);
			throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 요청해 주세요.");
		}

		Entry entry = new Entry();
		entry.code = String.format("%06d", random.nextInt(1_000_000));
		entry.sentAt = now;
		entry.expiresAt = now.plus(CODE_TTL);

		SimpleMailMessage msg = new SimpleMailMessage();
		msg.setFrom(from);
		msg.setTo(email);
		msg.setSubject("[Qubrit] 이메일 인증코드");
		msg.setText("인증코드: " + entry.code + "\n\n5분 안에 입력해 주세요.\n본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
		log.info("인증코드 메일 발송 시도: {} (from={})", email, from);
		try {
			mailSender.send(msg);
			log.info("인증코드 메일 발송 성공: {}", email);
		} catch (MailException e) {
			log.error("인증코드 메일 발송 실패: {}", email, e);
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "인증 메일을 보내지 못했어요. 잠시 후 다시 시도해 주세요.");
		}
		entries.put(email, entry);
	}

	public void verify(String rawEmail, String code) {
		String email = normalize(rawEmail);
		Entry entry = entries.get(email);
		if (entry == null || Instant.now().isAfter(entry.expiresAt)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증코드가 만료되었어요. 다시 요청해 주세요.");
		}
		synchronized (entry) {
			if (entry.attempts >= MAX_ATTEMPTS) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시도 횟수를 초과했어요. 코드를 다시 요청해 주세요.");
			}
			if (!entry.code.equals(code)) {
				entry.attempts++;
				log.warn("인증코드 불일치: {} ({}/{})", email, entry.attempts, MAX_ATTEMPTS);
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "인증코드가 일치하지 않아요.");
			}
			entry.verified = true;
			log.info("이메일 인증 완료: {}", email);
			entry.expiresAt = Instant.now().plus(VERIFIED_TTL);
		}
	}

	// 가입 시 호출: 인증이 끝난 이메일이 아니면 거부하고, 통과하면 1회용으로 소모한다.
	public void consumeVerified(String rawEmail) {
		String email = normalize(rawEmail);
		Entry entry = entries.get(email);
		if (entry == null || !entry.verified || Instant.now().isAfter(entry.expiresAt)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이메일 인증을 완료해 주세요.");
		}
		entries.remove(email);
	}

	@Scheduled(fixedDelay = 600_000)
	void evictExpired() {
		Instant now = Instant.now();
		entries.values().removeIf(e -> now.isAfter(e.expiresAt));
	}

	private String normalize(String email) {
		return email.trim().toLowerCase();
	}
}
