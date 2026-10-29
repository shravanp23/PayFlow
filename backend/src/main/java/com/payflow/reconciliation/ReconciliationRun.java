package com.payflow.reconciliation;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_runs")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReconciliationRun {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "run_id", nullable = false, unique = true)
    private String runId;

    @Column(nullable = false, length = 16)
    private String status; // RUNNING, COMPLETED

    @Column(name = "matched_count", nullable = false)
    private int matchedCount = 0;

    @Column(name = "mismatched_count", nullable = false)
    private int mismatchedCount = 0;

    @Column(name = "missing_count", nullable = false)
    private int missingCount = 0;

    @Column(name = "duplicate_count", nullable = false)
    private int duplicateCount = 0;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public static ReconciliationRun start(String runId) {
        ReconciliationRun r = new ReconciliationRun();
        r.runId = runId;
        r.status = "RUNNING";
        r.startedAt = Instant.now();
        return r;
    }

    public void complete() {
        this.status = "COMPLETED";
        this.completedAt = Instant.now();
    }
}
