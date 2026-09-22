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

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFileProvider() { return fileProvider; }
    public void setFileProvider(String fileProvider) { this.fileProvider = fileProvider; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<VideoFile> getVideos() { return videos; }
    public void setVideos(List<VideoFile> videos) { this.videos = videos; }
}
