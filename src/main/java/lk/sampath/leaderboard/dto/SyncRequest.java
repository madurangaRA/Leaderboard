package lk.sampath.leaderboard.dto;

import lombok.Data;

@Data
public class SyncRequest {
    private String projectKey;
    private boolean fullSync = false;
    private Integer daysBack;

    public SyncRequest() {
        this.daysBack = 90; // Default to 90 days
    }

    public SyncRequest(String projectKey, boolean fullSync, Integer daysBack) {
        this.projectKey = projectKey;
        this.fullSync = fullSync;
        this.daysBack = daysBack != null ? daysBack : 90;
    }
}
