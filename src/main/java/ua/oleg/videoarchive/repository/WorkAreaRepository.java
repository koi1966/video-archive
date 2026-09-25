package ua.oleg.videoarchive.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ua.oleg.videoarchive.model.WorkArea;

import java.util.Optional;

public interface WorkAreaRepository extends MongoRepository<WorkArea, String> {
    Optional<WorkArea> findByNameIgnoreCase(String name);
}
