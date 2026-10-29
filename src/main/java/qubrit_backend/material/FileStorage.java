package qubrit_backend.material;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 업로드 파일을 로컬 디렉터리에 저장한다. 저장 이름은 UUID라 원본 파일명으로 경로 조작이 불가능하다. */
@Component
public class FileStorage {

	private final Path root;

	public FileStorage(@Value("${storage.dir:uploads}") String dir) {
		this.root = Path.of(dir).toAbsolutePath().normalize();
	}

	public String save(byte[] data, String extension) {
		String name = UUID.randomUUID() + extension;
		try {
			Files.createDirectories(root);
			Files.write(root.resolve(name), data);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return name;
	}

	public byte[] read(String storagePath) {
		try {
			return Files.readAllBytes(resolve(storagePath));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public void delete(String storagePath) {
		try {
			Files.deleteIfExists(resolve(storagePath));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private Path resolve(String storagePath) {
		Path p = root.resolve(storagePath).normalize();
		if (!p.startsWith(root)) {
			throw new IllegalArgumentException("잘못된 경로");
		}
		return p;
	}
}
