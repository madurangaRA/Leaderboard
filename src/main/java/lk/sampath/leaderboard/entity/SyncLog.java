package lk.sampath.leaderboard.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_logs", indexes = {
        @Index(name = "idx_project_status", columnList = "project_id, status"),
        @Index(name = "idx_start_time", columnList = "start_time"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type")
    private SyncType syncType = SyncType.INCREMENTAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SyncStatus status = SyncStatus.STARTED;

    @Column(name = "records_processed")
    private Integer recordsProcessed = 0;

    @Column(name = "records_created")
    private Integer recordsCreated = 0;

    @Column(name = "records_updated")
    private Integer recordsUpdated = 0;

    @Column(name = "start_time", updatable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sync_details", columnDefinition = "JSON")
    private String syncDetails;

    @PrePersist
    protected void onCreate() {
        startTime = LocalDateTime.now();
    }

    public enum SyncType {
        FULL, INCREMENTAL, MANUAL
    }

    public enum SyncStatus {
        STARTED, SUCCESS, FAILED, PARTIAL
    }
}
