package com.payflow.reconciliation;

import com.payflow.reconciliation.dto.ReconciliationRunDetailResponse;
import com.payflow.reconciliation.dto.ReconciliationSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reconciliation")
@RequiredArgsConstructor
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER')")
    public ResponseEntity<ReconciliationSummaryResponse> run() {
        return ResponseEntity.status(HttpStatus.CREATED).body(reconciliationService.runReconciliation());
    }

    @GetMapping("/{runId}")
    @PreAuthorize("hasAnyRole('ADMIN','DEVELOPER','VIEWER')")
    public ReconciliationRunDetailResponse getRun(@PathVariable String runId) {
        return reconciliationService.getRun(runId);
    }
}
