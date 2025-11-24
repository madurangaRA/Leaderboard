package lk.sampath.leaderboard.services.impl;

import lk.sampath.leaderboard.entity.Project;
import lk.sampath.leaderboard.repository.ProjectRepository;
import lk.sampath.leaderboard.services.ProjectService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;

    @Override
    @Cacheable(value = "projects", key = "#projectKey")
    public Optional<Project> getProjectByKey(String projectKey) {
        log.debug("Fetching project by key: {}", projectKey);
        return projectRepository.findByProjectKey(projectKey);
    }

    @Override
    @Cacheable(value = "activeProjects")
    public List<Project> getAllActiveProjects() {
        log.debug("Fetching all active projects");
        return projectRepository.findByIsActiveTrue();
    }

    @Override
    @Transactional
    public Project saveProject(Project project) {
        log.info("Saving project: {}", project.getProjectName());
        return projectRepository.save(project);
    }

    @Override
    public Long countActiveProjects() {
        log.debug("Counting active projects");
        return projectRepository.countByIsActiveTrue();
    }
}
