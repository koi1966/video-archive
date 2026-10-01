package ua.oleg.videoarchive.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Document("video_records")
public class VideoRecord {
    @Id
    private String id;

    private String fileProvider;
    private LocalDate recordDate;
    private String companyName;
    private String title;
    private String description;
    private List<VideoFile> videos = new ArrayList<>();

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
     * Повертає сведения о том, кто передал файлы.
     * @return результат роботи методу (String)
     */
    public String getFileProvider() { return fileProvider; }
    /**
     * Встановлює сведения о том, кто передал файлы.
     * @param fileProvider параметр методу
     */
    public void setFileProvider(String fileProvider) { this.fileProvider = fileProvider; }
    /**
     * Повертає дату запису архива.
     * @return результат роботи методу (LocalDate)
     */
    public LocalDate getRecordDate() { return recordDate; }
    /**
     * Встановлює дату запису архива.
     * @param recordDate параметр методу
     */
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    /**
     * Повертає назва фірми.
     * @return результат роботи методу (String)
     */
    public String getCompanyName() { return companyName; }
    /**
     * Встановлює назва фірми.
     * @param companyName параметр методу
     */
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    /**
     * Повертає назву запису.
     * @return результат роботи методу (String)
     */
    public String getTitle() { return title; }
    /**
     * Встановлює назву запису.
     * @param title фільтр за назвою запису
     */
    public void setTitle(String title) { this.title = title; }
    /**
     * Повертає опис запису.
     * @return результат роботи методу (String)
     */
    public String getDescription() { return description; }
    /**
     * Встановлює опис запису.
     * @param description параметр методу
     */
    public void setDescription(String description) { this.description = description; }
    /**
     * Повертає список видео, связанных з записом.
     * @return результат роботи методу (List<VideoFile>)
     */
    public List<VideoFile> getVideos() { return videos; }
    /**
     * Встановлює список видео, связанных з записом.
     * @param videos параметр методу
     */
    public void setVideos(List<VideoFile> videos) { this.videos = videos; }
}
