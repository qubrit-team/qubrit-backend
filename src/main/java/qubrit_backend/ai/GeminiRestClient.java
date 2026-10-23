package qubrit_backend.ai;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class GeminiRestClient implements GeminiClient {

	private final GeminiProperties props;
	private final JsonMapper mapper;
	private final RestClient http;

	public GeminiRestClient(GeminiProperties props, JsonMapper mapper) {
		this.props = props;
		this.mapper = mapper;
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(Duration.ofSeconds(10));
		factory.setReadTimeout(Duration.ofSeconds(90)); // 문서 기반 생성은 오래 걸릴 수 있다
		this.http = RestClient.builder().baseUrl(props.baseUrl()).requestFactory(factory).build();
	}

	@Override
	public String generateJson(String prompt, Attachment attachment, Map<String, Object> responseSchema) {
		if (props.apiKey() == null || props.apiKey().isBlank()) {
			throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI 기능이 설정되지 않았습니다.");
		}
		List<Object> parts = new ArrayList<>();
		if (attachment != null) {
			parts.add(Map.of("inline_data", Map.of("mime_type", attachment.mimeType(),
					"data", Base64.getEncoder().encodeToString(attachment.data()))));
		}
		parts.add(Map.of("text", prompt));
		Map<String, Object> body = Map.of(
				"contents", List.of(Map.of("role", "user", "parts", parts)),
				"generationConfig", Map.of("responseMimeType", "application/json", "responseSchema", responseSchema));

		String raw;
		try {
			raw = http.post().uri("/v1beta/models/{model}:generateContent", props.model())
					.header("x-goog-api-key", props.apiKey())
					.contentType(MediaType.APPLICATION_JSON)
					.body(mapper.writeValueAsString(body)) // 미리 직렬화해서 Content-Length를 보낸다
					.retrieve()
					.body(String.class);
		} catch (RestClientException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 서버 호출에 실패했습니다.");
		}
		try {
			JsonNode text = mapper.readTree(raw).path("candidates").path(0).path("content").path("parts").path(0)
					.path("text");
			if (text.isMissingNode() || text.isNull()) {
				throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI가 응답을 만들지 못했습니다.");
			}
			return text.asString();
		} catch (ResponseStatusException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI 응답을 해석하지 못했습니다.");
		}
	}
}
