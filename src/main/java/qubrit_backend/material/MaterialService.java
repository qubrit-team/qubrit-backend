package qubrit_backend.material;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import qubrit_backend.auth.domain.User;
import qubrit_backend.auth.domain.UserRepository;
import qubrit_backend.quiz.dto.PageResponse;
import qubrit_backend.subject.SubjectRepository;

@Service
public class MaterialService {

	// 확장자 → 허용하는 MIME. 클라이언트가 보낸 Content-Type은 믿지 않고 확장자로 정한다.
	private static final Map<String, String> TYPES = Map.of(
			"pdf", "application/pdf",
			"txt", "text/plain",
			"md", "text/plain",
			"png", "image/png",
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg");

	private final MaterialRepository materialRepository;
	private final SubjectRepository subjectRepository;
	private final UserRepository userRepository;
	private final FileStorage storage;

	public MaterialService(MaterialRepository materialRepository, SubjectRepository subjectRepository,
			UserRepository userRepository, FileStorage storage) {
		this.materialRepository = materialRepository;
		this.subjectRepository = subjectRepository;
		this.userRepository = userRepository;
		this.storage = storage;
	}

	@Transactional
	public MaterialDto upload(Long userId, MultipartFile file, String title, String subject) {
		if (file.isEmpty()) {
			throw bad("파일을 선택해 주세요.");
		}
		String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
		String ext = original.contains(".") ? original.substring(original.lastIndexOf('.') + 1).toLowerCase() : "";
		String mime = TYPES.get(ext);
		if (mime == null) {
			throw bad("지원하지 않는 파일 형식입니다. (pdf, txt, md, png, jpg)");
		}
		var subjectEntity = subjectRepository.findByName(subject.trim()).orElseThrow(() -> bad("존재하지 않는 과목입니다."));
		String name = (title == null || title.isBlank()) ? original : title.trim();
		if (name.length() > 100) {
			throw bad("제목은 100자 이하여야 합니다.");
		}

		byte[] data;
		try {
			data = file.getBytes();
		} catch (IOException e) {
			throw bad("파일을 읽지 못했습니다.");
		}
		String text = mime.equals("text/plain") ? new String(data, StandardCharsets.UTF_8) : null;
		String path = storage.save(data, "." + ext);
		Material saved = materialRepository.save(
				new Material(userId, subjectEntity, name, original, mime, data.length, path, text));
		return toDto(saved, userId, nicknameOf(userId));
	}

	@Transactional(readOnly = true)
	public PageResponse<MaterialDto> list(Long userId, String subject, String q, int page, int size) {
		Page<Material> result = materialRepository.search(subject == null ? "" : subject.trim(),
				q == null ? "" : q.trim(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));
		Map<Long, String> nicknames = userRepository
				.findAllById(result.getContent().stream().map(Material::getUploaderId).distinct().toList()).stream()
				.collect(Collectors.toMap(User::getId, User::getNickname));
		return PageResponse.of(result, m -> toDto(m, userId, nicknames.get(m.getUploaderId())));
	}

	@Transactional(readOnly = true)
	public MaterialDto get(Long userId, Long id) {
		Material m = find(id);
		return toDto(m, userId, nicknameOf(m.getUploaderId()));
	}

	@Transactional
	public void delete(Long userId, Long id) {
		Material m = find(id);
		if (!m.getUploaderId().equals(userId)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "올린 사람만 삭제할 수 있습니다.");
		}
		materialRepository.delete(m);
		storage.delete(m.getStoragePath());
	}

	public Material find(Long id) {
		return materialRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "자료를 찾을 수 없습니다."));
	}

	public byte[] read(Material m) {
		return storage.read(m.getStoragePath());
	}

	private String nicknameOf(Long userId) {
		return userRepository.findById(userId).map(User::getNickname).orElse("");
	}

	private MaterialDto toDto(Material m, Long userId, String nickname) {
		return new MaterialDto(m.getId(), m.getTitle(), m.getSubject().getName(), m.getFileName(),
				m.getContentType(), m.getSize(), nickname, m.getUploaderId().equals(userId), m.getCreatedAt());
	}

	private static ResponseStatusException bad(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}
}
