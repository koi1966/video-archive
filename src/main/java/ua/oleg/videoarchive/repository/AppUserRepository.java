package ua.oleg.videoarchive.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ua.oleg.videoarchive.model.AppUser;

import java.util.Optional;

public interface AppUserRepository extends MongoRepository<AppUser, String> {
    Optional<AppUser> findByUsername(String username);

    long countByWorkAreaId(String workAreaId);
}
