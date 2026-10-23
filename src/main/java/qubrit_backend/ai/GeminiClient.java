package qubrit_backend.ai;

import java.util.Map;

/** Gemini 호출은 이 인터페이스 한 곳으로만 한다. 서비스는 구현체를 몰라도 되어서 테스트에서 교체할 수 있다. */
public interface GeminiClient {

	/** 파일(PDF/이미지)은 모델에 그대로 첨부한다. */
	record Attachment(String mimeType, byte[] data) {
	}

	/**
	 * 프롬프트를 보내고, responseSchema에 맞는 JSON 문자열을 돌려받는다.
	 *
	 * @param attachment 없으면 null
	 * @throws org.springframework.web.server.ResponseStatusException 키 미설정(503), 호출/응답 오류(502)
	 */
	String generateJson(String prompt, Attachment attachment, Map<String, Object> responseSchema);
}
