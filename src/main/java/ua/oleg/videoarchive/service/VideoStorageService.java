package ua.oleg.videoarchive.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.time.Instant;
import java.util.UUID;

@Service
public class VideoStorageService {
    private final Path root;

    public VideoStorageService(@Value("${video.storage.path}") String storagePath) throws IOException {
        this.root = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(root);
    }

    public VideoFile store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Empty video file");
        }

        String original = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "video" : file.getOriginalFilename());

        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) extension = original.substring(dot);

        String stored = UUID.randomUUID() + extension;
        Path destination = root.resolve(stored).normalize();

        if (!destination.getParent().equals(root)) {
            throw new IOException("Invalid file path");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
        }

        return new VideoFile(
                UUID.randomUUID().toString(),
                null,
                original,
                stored,
                stored,
                Files.size(destination),
                Instant.now()
        );
    }

    public Path resolve(VideoFile video) {
        Path path = root.resolve(video.getRelativePath()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid stored path");
        }
        return path;
    }

    public void delete(VideoFile video) throws IOException {
        Files.deleteIfExists(resolve(video));
    }

    public Path getRoot() {
        return root;
    }
}
