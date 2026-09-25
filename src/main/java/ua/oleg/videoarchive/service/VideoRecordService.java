package ua.oleg.videoarchive.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import ua.oleg.videoarchive.model.VideoFile;
import ua.oleg.videoarchive.model.VideoRecord;
import ua.oleg.videoarchive.model.WorkArea;
import ua.oleg.videoarchive.repository.VideoRecordRepository;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@Service
public class VideoRecordService {
    private final VideoRecordRepository repository;
    private final VideoStorageService storage;

    public VideoRecordService(VideoRecordRepository repository, VideoStorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public List<VideoRecord> search(String provider, String company, String title, LocalDate date) {
        Stream<VideoRecord> stream = repository.findAll().stream();

        if (provider != null && !provider.isBlank())
            stream = stream.filter(r -> contains(r.getFileProvider(), provider));
        if (company != null && !company.isBlank())
            stream = stream.filter(r -> contains(r.getCompanyName(), company));
        if (title != null && !title.isBlank())
            stream = stream.filter(r -> contains(r.getTitle(), title));
        if (date != null)
            stream = stream.filter(r -> date.equals(r.getRecordDate()));

        return stream.sorted(Comparator.comparing(VideoRecord::getRecordDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private boolean contains(String value, String search) {
        return value != null && value.toLowerCase(Locale.ROOT)
                .contains(search.toLowerCase(Locale.ROOT));
    }

    public VideoRecord save(VideoRecord record) {
        if (record.getVideos() == null) record.setVideos(new ArrayList<>());
        return repository.save(record);
    }

    public VideoRecord find(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Record not found"));
    }

    public void addVideos(String recordId, MultipartFile[] files, WorkArea workArea) throws IOException {
        VideoRecord record = find(recordId);
        if (files != null) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    record.getVideos().add(storage.store(file, workArea));
                }
            }
        }
        repository.save(record);
    }

    public VideoFile findVideo(String recordId, String videoId) {
        return find(recordId).getVideos().stream()
                .filter(v -> videoId.equals(v.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Video not found"));
    }

    public void deleteVideo(String recordId, String videoId) throws IOException {
        VideoRecord record = find(recordId);
        VideoFile video = findVideo(recordId, videoId);
        storage.delete(video);
        record.getVideos().removeIf(v -> videoId.equals(v.getId()));
        repository.save(record);
    }

    public void deleteRecord(String recordId) throws IOException {
        VideoRecord record = find(recordId);
        for (VideoFile video : record.getVideos()) {
            storage.delete(video);
        }
        repository.delete(record);
    }
}