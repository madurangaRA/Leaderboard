package lk.sampath.leaderboard.services;


import lk.sampath.leaderboard.dto.SyncResponse;
import lk.sampath.leaderboard.entity.Project;

/**
 * Service interface for syncing data from SonarQube to database
 */
public interface SonarQubeSyncService {

    /**
     * Sync all projects and their data from SonarQube
     *
     * @param fullSync If true, performs full sync for historical period.
     *                 If false, performs incremental sync from last successful sync.
     * @return SyncResponse with statistics and status
     */
    SyncResponse syncAllProjects(boolean fullSync);

    /**
     * Sync a specific project and its data
     *
     * @param project The project to sync
     * @param fullSync If true, performs full sync. If false, incremental sync.
     * @return SyncStats with details of what was synced
     */
    SyncResponse.SyncStats syncProject(Project project, boolean fullSync);
}
