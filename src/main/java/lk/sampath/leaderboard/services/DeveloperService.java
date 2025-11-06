package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.entity.Developer;

import java.util.List;
import java.util.Optional;

public interface DeveloperService {
    Optional<Developer> getDeveloperByAuthorKey(String authorKey);
    List<Developer> getAllActiveDevelopers();
    Developer saveDeveloper(Developer developer);
    Long countActiveDevelopers();
}
