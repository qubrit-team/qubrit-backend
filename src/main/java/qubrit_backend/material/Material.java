package qubrit_backend.material;

import java.time.Instant;

import jakarta.persistence.*;
import qubrit_backend.subject.Subject;

@Entity
@Table(name = "materials")
public class Material {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "uploader_id", nullable = false)
	private Long uploaderId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "subject_id")
	private Subject subject;

	@Column(nullable = false)
	private String title;

	@Column(name = "file_name", nullable = false)
	private String fileName;

	@Column(name = "content_type", nullable = false)
	private String contentType;

	private long size;

	// 저장소 안의 상대 경로 (원본 파일명은 경로에 쓰지 않는다)
	@Column(name = "storage_path", nullable = false)
	private String storagePath;

	// txt/md만 추출해 둔다. PDF/이미지는 Gemini에 파일 그대로 전달
	@Column(name = "extracted_text", columnDefinition = "text")
	private String extractedText;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	protected Material() {
	}

	public Material(Long uploaderId, Subject subject, String title, String fileName, String contentType, long size,
			String storagePath, String extractedText) {
		this.uploaderId = uploaderId;
		this.subject = subject;
		this.title = title;
		this.fileName = fileName;
		this.contentType = contentType;
		this.size = size;
		this.storagePath = storagePath;
		this.extractedText = extractedText;
	}

	public Long getId() { return id; }
	public Long getUploaderId() { return uploaderId; }
	public Subject getSubject() { return subject; }
	public String getTitle() { return title; }
	public String getFileName() { return fileName; }
	public String getContentType() { return contentType; }
	public long getSize() { return size; }
	public String getStoragePath() { return storagePath; }
	public String getExtractedText() { return extractedText; }
	public Instant getCreatedAt() { return createdAt; }
}
