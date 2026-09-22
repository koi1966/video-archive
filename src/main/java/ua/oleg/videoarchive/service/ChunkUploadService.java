package ua.oleg.videoarchive.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.VideoRecord;

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

    /**
     * Должен совпадать с JavaScript:
     *
     * const CHUNK_SIZE = 8 * 1024 * 1024;
     */
    private static final long CHUNK_SIZE = 8L * 1024L * 1024L;

    /**
     * Lock только для конкретной загрузки.
     * Поэтому загрузки разных файлов не блокируют друг друга.
     */
    private final ConcurrentHashMap<String, Object> uploadLocks =
            new ConcurrentHashMap<>();

    private final VideoRecordService records;
    private final VideoStorageService storage;

    public ChunkUploadService(VideoRecordService records,
                              VideoStorageService storage) {
        this.records = records;
        this.storage = storage;
    }

    /**
     * Записывает один chunk.
     *
     * @return true, если это последний chunk и файл полностью собран
     */
    public boolean writeChunk(
            String recordId,
            String uploadId,
            String originalName,
            long totalSize,
            int chunkNumber,
            int totalChunks,
            MultipartFile chunk
    ) throws IOException {

        validateParameters(
                recordId,
                uploadId,
                originalName,
                totalSize,
                chunkNumber,
                totalChunks,
                chunk
        );

        Object lock = uploadLocks.computeIfAbsent(
                uploadId,
                key -> new Object()
        );

        synchronized (lock) {
            try {
                return writeChunkInternal(
                        recordId,
                        uploadId,
                        originalName,
                        totalSize,
                        chunkNumber,
                        totalChunks,
                        chunk
                );
            } finally {
                uploadLocks.remove(uploadId, lock);
            }
        }
    }

    /**
     * Проверка входных параметров.
     */
    private void validateParameters(
            String recordId,
            String uploadId,
            String originalName,
            long totalSize,
            int chunkNumber,
            int totalChunks,
            MultipartFile chunk
    ) {

        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("recordId is empty");
        }

        if (uploadId == null || uploadId.isBlank()) {
            throw new IllegalArgumentException("uploadId is empty");
        }

        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("originalName is empty");
        }

        if (totalSize <= 0) {
            throw new IllegalArgumentException("Invalid totalSize");
        }

        if (totalChunks <= 0) {
            throw new IllegalArgumentException("Invalid totalChunks");
        }

        if (chunkNumber < 0 || chunkNumber >= totalChunks) {
            throw new IllegalArgumentException(
                    "Invalid chunkNumber: " + chunkNumber
            );
        }

        if (chunk == null || chunk.isEmpty()) {
            throw new IllegalArgumentException("Empty chunk");
        }

        /*
         * Все chunks, кроме последнего, должны иметь размер CHUNK_SIZE.
         */
        if (chunkNumber < totalChunks - 1
                && chunk.getSize() != CHUNK_SIZE) {

            throw new IllegalArgumentException(
                    "Invalid chunk size: " + chunk.getSize()
                            + ", expected: " + CHUNK_SIZE
            );
        }

        /*
         * Размер последнего chunk не должен быть больше CHUNK_SIZE.
         */
        if (chunk.getSize() > CHUNK_SIZE) {
            throw new IllegalArgumentException(
                    "Chunk is larger than CHUNK_SIZE"
            );
        }
    }

    /**
     * Непосредственная запись chunk.
     */
    private boolean writeChunkInternal(
            String recordId,
            String uploadId,
            String originalName,
            long totalSize,
            int chunkNumber,
            int totalChunks,
            MultipartFile chunk
    ) throws IOException {

        String safeName = Path.of(originalName)
                .getFileName()
                .toString();

        Path root = storage.getRoot().toAbsolutePath().normalize();

        Path part = root
                .resolve(".upload-" + uploadId + ".part")
                .normalize();

        /*
         * Защита от выхода за пределы storage root.
         */
        if (!part.getParent().equals(root)) {
            throw new IOException("Invalid upload path");
        }

        /*
         * Позиция chunk в итоговом файле.
         */
        long offset = ((long) chunkNumber) * CHUNK_SIZE;

        /*
         * Проверяем, что chunk не выходит за пределы
         * заявленного размера файла.
         */
        if (offset >= totalSize && chunkNumber != totalChunks - 1) {
            throw new IOException("Chunk offset exceeds total file size");
        }

        if (offset + chunk.getSize() > totalSize) {
            throw new IOException(
                    "Chunk exceeds expected total file size"
            );
        }

        /*
         * Записываем chunk непосредственно в нужную позицию файла.
         */
        try (RandomAccessFile raf =
                     new RandomAccessFile(part.toFile(), "rw")) {

            raf.seek(offset);

            try (var in = chunk.getInputStream()) {

                byte[] buffer = new byte[1024 * 1024];

                int n;

                while ((n = in.read(buffer)) != -1) {
                    raf.write(buffer, 0, n);
                }
            }
        }

        /*
         * Последний chunk означает завершение загрузки.
         */
        if (chunkNumber == totalChunks - 1) {

            long actualSize = Files.size(part);

            if (actualSize != totalSize) {
                throw new IOException(
                        "Uploaded file size does not match expected size. "
                                + "Expected: " + totalSize
                                + ", actual: " + actualSize
                );
            }

            /*
             * Определяем расширение исходного файла.
             */
            String extension = "";

            int dot = safeName.lastIndexOf('.');

            if (dot > 0 && dot < safeName.length() - 1) {
                extension = safeName.substring(dot);
            }

            /*
             * Физическое имя файла в storage.
             */
            String storedName =
                    UUID.randomUUID() + extension;

            Path finalPath =
                    root.resolve(storedName).normalize();

            if (!finalPath.getParent().equals(root)) {
                throw new IOException("Invalid final file path");
            }

            /*
             * Перемещаем временный файл в окончательное место.
             */
            Files.move(
                    part,
                    finalPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            /*
             * Добавляем информацию о видео в запись.
             */
            VideoRecord record = records.find(recordId);

            record.getVideos().add(
                    new VideoFile(
                            UUID.randomUUID().toString(),
                            uploadId,
                            safeName,
                            storedName,
                            storedName,
                            totalSize,
                            Instant.now()
                    )
            );

            records.save(record);

            return true;
        }

        return false;
    }
}