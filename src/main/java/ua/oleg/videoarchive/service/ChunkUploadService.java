package ua.oleg.videoarchive.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.VideoRecord;
import ua.oleg.videoarchive.model.WorkArea;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChunkUploadService {
    private static final long CHUNK_SIZE = 8L * 1024L * 1024L;

    private final ConcurrentHashMap<String, Object> uploadLocks = new ConcurrentHashMap<>();

    private final VideoRecordService records;
    private final VideoStorageService storage;

    public ChunkUploadService(VideoRecordService records, VideoStorageService storage) {
        this.records = records;
        this.storage = storage;
    }

    public boolean writeChunk(
            String recordId,
            String uploadId,
            String originalName,
            long totalSize,
            int chunkNumber,
            int totalChunks,
            MultipartFile chunk,
            WorkArea workArea
    ) throws IOException {
        validateParameters(recordId, uploadId, originalName, totalSize,
                chunkNumber, totalChunks, chunk, workArea);

        Object lock = uploadLocks.computeIfAbsent(uploadId, key -> new Object());

        synchronized (lock) {
            try {
                return writeChunkInternal(recordId, uploadId, originalName, totalSize,
                        chunkNumber, totalChunks, chunk, workArea);
            } finally {
                uploadLocks.remove(uploadId, lock);
            }
        }
    }

    private void validateParameters(
            String recordId, String uploadId, String originalName, long totalSize,
            int chunkNumber, int totalChunks, MultipartFile chunk, WorkArea workArea) {
        if (recordId == null || recordId.isBlank()) throw new IllegalArgumentException("recordId is empty");
        if (uploadId == null || uploadId.isBlank()) throw new IllegalArgumentException("uploadId is empty");
        if (originalName == null || originalName.isBlank()) throw new IllegalArgumentException("originalName is empty");
        if (totalSize <= 0) throw new IllegalArgumentException("Invalid totalSize");
        if (totalChunks <= 0) throw new IllegalArgumentException("Invalid totalChunks");
        if (chunkNumber < 0 || chunkNumber >= totalChunks) {
            throw new IllegalArgumentException("Invalid chunkNumber: " + chunkNumber);
        }
        if (chunk == null || chunk.isEmpty()) throw new IllegalArgumentException("Empty chunk");
        if (workArea == null) throw new IllegalArgumentException("WorkArea is required");

        if (chunkNumber < totalChunks - 1 && chunk.getSize() != CHUNK_SIZE) {
            throw new IllegalArgumentException("Invalid chunk size: " + chunk.getSize()
                    + ", expected: " + CHUNK_SIZE);
        }
        if (chunk.getSize() > CHUNK_SIZE) {
            throw new IllegalArgumentException("Chunk is larger than CHUNK_SIZE");
        }
    }

    private boolean writeChunkInternal(
            String recordId, String uploadId, String originalName, long totalSize,
            int chunkNumber, int totalChunks, MultipartFile chunk, WorkArea workArea)
            throws IOException {

        String safeName = Path.of(originalName).getFileName().toString();
        Path root = storage.rootFor(workArea).toAbsolutePath().normalize();

        Path part = root.resolve(".upload-" + uploadId + ".part").normalize();
        if (!part.getParent().equals(root)) throw new IOException("Invalid upload path");

        long offset = ((long) chunkNumber) * CHUNK_SIZE;
        if (offset >= totalSize && chunkNumber != totalChunks - 1) {
            throw new IOException("Chunk offset exceeds total file size");
        }
        if (offset + chunk.getSize() > totalSize) {
            throw new IOException("Chunk exceeds expected total file size");
        }

        try (RandomAccessFile raf = new RandomAccessFile(part.toFile(), "rw")) {
            raf.seek(offset);
            try (var in = chunk.getInputStream()) {
                byte[] buffer = new byte[1024 * 1024];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    raf.write(buffer, 0, n);
                }
            }
        }

        if (chunkNumber == totalChunks - 1) {
            long actualSize = Files.size(part);
            if (actualSize != totalSize) {
                throw new IOException("Uploaded file size does not match expected size. Expected: "
                        + totalSize + ", actual: " + actualSize);
            }

            String extension = "";
            int dot = safeName.lastIndexOf('.');
            if (dot > 0 && dot < safeName.length() - 1) extension = safeName.substring(dot);

            String storedName = UUID.randomUUID() + extension;
            Path finalPath = root.resolve(storedName).normalize();
            if (!finalPath.getParent().equals(root)) throw new IOException("Invalid final file path");

            Files.move(part, finalPath, StandardCopyOption.REPLACE_EXISTING);

            VideoRecord record = records.find(recordId);
            record.getVideos().add(new VideoFile(
                    UUID.randomUUID().toString(),
                    uploadId,
                    safeName,
                    storedName,
                    workArea.getId(),
                    root.toString(),
                    storedName,
                    totalSize,
                    Instant.now()
            ));
            records.save(record);
            return true;
        }

        return false;
    }
}