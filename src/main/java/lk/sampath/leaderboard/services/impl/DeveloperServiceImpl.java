package lk.sampath.leaderboard.services.impl;

import lk.sampath.leaderboard.entity.Developer;
import lk.sampath.leaderboard.repository.DeveloperRepository;
import lk.sampath.leaderboard.services.DeveloperService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@Slf4j
public class DeveloperServiceImpl implements DeveloperService {

    @Autowired
    private DeveloperRepository developerRepository;

    @Override
    @Cacheable(value = "developers", key = "#authorKey")
    public Optional<Developer> getDeveloperByAuthorKey(String authorKey) {
        log.debug("Fetching developer by author key: {}", authorKey);
        return developerRepository.findByAuthorKey(authorKey);
    }

    @Override
    @Cacheable(value = "activeDevelopers")
    public List<Developer> getAllActiveDevelopers() {
        log.debug("Fetching all active developers");
        return developerRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional
    public Developer saveDeveloper(Developer developer) {
        log.info("Saving developer: {}", developer.getDisplayName());
        return developerRepository.save(developer);
    }

    @Override
    public Long countActiveDevelopers() {
        log.debug("Counting active developers");
        return developerRepository.countByIsActiveTrue();
    }
}
