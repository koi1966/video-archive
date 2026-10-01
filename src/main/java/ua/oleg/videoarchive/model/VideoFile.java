package ua.oleg.videoarchive.model;

import java.time.Instant;

public class VideoFile {
    private String id;

    /**
     * Уникальный ідентифікатор конкретной завантаження.
     * Один uploadId використовується для всех chunks одного файлу.
     */
    private String uploadId;

    private String originalName;
    private String storedName;

    /** Идентификатор WorkArea, у patch якого записан файл. */
    private String workAreaId;

    /**
     * Patch, действовавший у момент завантаження. Нужен, щоб изменение
     * поточного patch у WorkArea не ломало доступ к старым видео.
     */
    private String storagePatch;

    /** Путь щодо storagePatch, обычно это storedName. */
    private String relativePath;

    private long size;
    private Instant uploadedAt;

    /**
     * Створює об’єкт VideoFile і отримує необхідні зависимости із Spring-контейнера.
     */
    public VideoFile() {
    }

    /**
     * Створює об’єкт VideoFile і отримує необхідні зависимости із Spring-контейнера.
     * @param id ідентифікатор об’єкта
     * @param uploadId унікальний ідентифікатор поточної chunk-завантаження
     * @param originalName параметр методу
     * @param storedName параметр методу
     * @param workAreaId ідентифікатор робочої зони користувача
     * @param storagePatch параметр методу
     * @param relativePath шлях файлу щодо корневого каталогу
     * @param size параметр методу
     * @param uploadedAt параметр методу
     */
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
     * Конструктор для совместимости со старыми даними/кодом.
     * workAreaId отсутствует, поэтому такой файл будет искаться
     * у старом общем video.storage.path.
     */
    public VideoFile(String id, String uploadId, String originalName,
                     String storedName, String relativePath,
                     long size, Instant uploadedAt) {
        this(id, uploadId, originalName, storedName, null, null, relativePath, size, uploadedAt);
    }

    /**
     * Повертає ідентифікатор об’єкта.
     * @return результат роботи методу (String)
     */
    public String getId() { return id; }
    /**
     * Встановлює ідентифікатор об’єкта.
     * @param id ідентифікатор об’єкта
     */
    public void setId(String id) { this.id = id; }

    /**
     * Повертає ідентифікатор chunk-завантаження.
     * @return результат роботи методу (String)
     */
    public String getUploadId() { return uploadId; }
    /**
     * Встановлює ідентифікатор chunk-завантаження.
     * @param uploadId унікальний ідентифікатор поточної chunk-завантаження
     */
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    /**
     * Повертає исходное ім’я відеофайлу, выбранное пользователем.
     * @return результат роботи методу (String)
     */
    public String getOriginalName() { return originalName; }
    /**
     * Встановлює исходное ім’я відеофайлу.
     * @param originalName параметр методу
     */
    public void setOriginalName(String originalName) { this.originalName = originalName; }

    /**
     * Повертає физическое ім’я файлу на диске.
     * @return результат роботи методу (String)
     */
    public String getStoredName() { return storedName; }
    /**
     * Встановлює физическое ім’я файлу на диске.
     * @param storedName параметр методу
     */
    public void setStoredName(String storedName) { this.storedName = storedName; }

    /**
     * Повертає ідентифікатор робочої зони.
     * @return результат роботи методу (String)
     */
    public String getWorkAreaId() { return workAreaId; }
    /**
     * Встановлює ідентифікатор робочої зони.
     * @param workAreaId ідентифікатор робочої зони користувача
     */
    public void setWorkAreaId(String workAreaId) { this.workAreaId = workAreaId; }

    /**
     * Повертає patch, збережений у момент завантаження файлу.
     * @return результат роботи методу (String)
     */
    public String getStoragePatch() { return storagePatch; }
    /**
     * Встановлює фізичний patch, использованный для сохранения файлу.
     * @param storagePatch параметр методу
     */
    public void setStoragePatch(String storagePatch) { this.storagePatch = storagePatch; }

    /**
     * Повертає шлях файлу щодо storagePatch.
     * @return результат роботи методу (String)
     */
    public String getRelativePath() { return relativePath; }
    /**
     * Встановлює относительный шлях файлу.
     * @param relativePath шлях файлу щодо корневого каталогу
     */
    public void setRelativePath(String relativePath) { this.relativePath = relativePath; }

    /**
     * Повертає розмір видео у байтах.
     * @return результат роботи методу (long)
     */
    public long getSize() { return size; }
    /**
     * Встановлює розмір видео у байтах.
     * @param size параметр методу
     */
    public void setSize(long size) { this.size = size; }

    /**
     * Повертає момент завантаження видео.
     * @return результат роботи методу (Instant)
     */
    public Instant getUploadedAt() { return uploadedAt; }
    /**
     * Встановлює момент завантаження видео.
     * @param uploadedAt параметр методу
     */
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }
}
