package ua.oleg.videoarchive.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ua.oleg.videoarchive.model.VideoRecord;

import java.time.LocalDate;
import java.util.List;

public interface VideoRecordRepository extends MongoRepository<VideoRecord, String> {
    List<VideoRecord> findByCompanyNameContainingIgnoreCase(String companyName);
    List<VideoRecord> findByTitleContainingIgnoreCase(String title);
    List<VideoRecord> findByFileProviderContainingIgnoreCase(String provider);
    List<VideoRecord> findByRecordDate(LocalDate date);
}
