package ua.oleg.videoarchive.model;

import java.time.Instant;
public class VideoFile { private String id;
    /**
     * Уникальный идентификатор конкретной загрузки.
     * Один uploadId используется для всех chunks одного файла.
     */

    private String uploadId;
    private String originalName;
    private String storedName;
    private String relativePath;
    private long size;
    private Instant uploadedAt;
    public VideoFile() { }
    public VideoFile(String id, String uploadId, String originalName,
                     String storedName, String relativePath,
                     long size, Instant uploadedAt)
    { this.id = id;
        this.uploadId = uploadId;
        this.originalName = originalName;
        this.storedName = storedName;
        this.relativePath = relativePath;
        this.size = size;
        this.uploadedAt = uploadedAt; }
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public String getStoredName() { return storedName; }
    public void setStoredName(String storedName) { this.storedName = storedName; }
    public String getRelativePath() { return relativePath; }
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }
    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }
    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}