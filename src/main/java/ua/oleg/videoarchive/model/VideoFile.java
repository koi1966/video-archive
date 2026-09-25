package ua.oleg.videoarchive.model;

import java.time.Instant;

public class VideoFile {
    private String id;

    /**
     * Уникальный идентификатор конкретной загрузки.
     * Один uploadId используется для всех chunks одного файла.
     */
    private String uploadId;

    private String originalName;
    private String storedName;

    /** Идентификатор WorkArea, в patch которого записан файл. */
    private String workAreaId;

    /**
     * Patch, действовавший в момент загрузки. Нужен, чтобы изменение
     * текущего patch у WorkArea не ломало доступ к старым видео.
     */
    private String storagePatch;

    /** Путь относительно storagePatch, обычно это storedName. */
    private String relativePath;

    private long size;
    private Instant uploadedAt;

    public VideoFile() {
    }

    public VideoFile(String id, String uploadId, String originalName,
                     String storedName, String workAreaId, String storagePatch,
                     String relativePath, long size, Instant uploadedAt) {
        this.id = id;
        this.uploadId = uploadId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.workAreaId = workAreaId;
        this.storagePatch = storagePatch;
        this.relativePath = relativePath;
        this.size = size;
        this.uploadedAt = uploadedAt;
    }

    /**
     * Конструктор для совместимости со старыми данными/кодом.
     * workAreaId отсутствует, поэтому такой файл будет искаться
     * в старом общем video.storage.path.
     */
    public VideoFile(String id, String uploadId, String originalName,
                     String storedName, String relativePath,
                     long size, Instant uploadedAt) {
        this(id, uploadId, originalName, storedName, null, null, relativePath, size, uploadedAt);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }

    public String getWorkAreaId() { return workAreaId; }
    public void setWorkAreaId(String workAreaId) { this.workAreaId = workAreaId; }

    public String getStoragePatch() { return storagePatch; }
    public void setStoragePatch(String storagePatch) { this.storagePatch = storagePatch; }

    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}