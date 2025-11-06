package lk.sampath.leaderboard.dto;

import lombok.Data;

@Data
public class SyncResponse {
    private boolean success;
    private String message;
    private Integer syncLogId;
    private SyncStats stats;

    @Data
    public static class SyncStats {
        private int projectsProcessed;
        private int developersCreated;
        private int issuesCreated;
        private int issuesUpdated;
        private int metricsCreated;
        private long durationMs;
    }

    public static SyncResponse success(String message, Integer syncLogId, SyncStats stats) {
        SyncResponse response = new SyncResponse();
        response.setSuccess(true);
        response.setMessage(message);
        response.setSyncLogId(syncLogId);
        response.setStats(stats);
        return response;
    }

    public static SyncResponse failure(String message) {
        SyncResponse response = new SyncResponse();
        response.setSuccess(false);
        response.setMessage(message);
        return response;
    }
}
