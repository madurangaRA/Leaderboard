package lk.sampath.leaderboard.services;

import lk.sampath.leaderboard.entity.Project;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    Optional<Project> getProjectByKey(String projectKey);
    List<Project> getAllActiveProjects();
    Project saveProject(Project project);
    Long countActiveProjects();
}
