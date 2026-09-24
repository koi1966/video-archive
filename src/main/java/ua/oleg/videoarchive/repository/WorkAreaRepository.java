package ua.oleg.videoarchive.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ua.oleg.videoarchive.model.WorkArea;

public interface WorkAreaRepository extends MongoRepository<WorkArea, String> {
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);
}
