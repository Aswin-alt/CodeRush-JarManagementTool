package com.coderush.jaranalyzer.common.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for all analysis results.
 * Contains common properties and metadata for analysis outcomes.
 * 
 * This abstract class ensures consistency in result structure across all
 * six features and provides a foundation for rich reporting capabilities.
 */
public abstract class AnalysisResult {
    
    private final String requestId;
    private final AnalysisType analysisType;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final AnalysisStatus status;
    private final Map<String, Object> metadata;
    private final List<String> warnings;
    private final String errorMessage;
    
    /**
     * Analysis execution status.
     */
    public enum AnalysisStatus {
        SUCCESS,     // Analysis completed successfully
        PARTIAL,     // Analysis completed with some issues
        FAILED,      // Analysis failed
        CANCELLED    // Analysis was cancelled
    }
    
    /**
     * Constructor for successful analysis result.
     */
    protected AnalysisResult(String requestId, AnalysisType analysisType, 
                           LocalDateTime startTime, LocalDateTime endTime,
                           Map<String, Object> metadata, List<String> warnings) {
        this.requestId = requestId;
        this.analysisType = analysisType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = warnings.isEmpty() ? AnalysisStatus.SUCCESS : AnalysisStatus.PARTIAL;
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        this.warnings = warnings != null ? new ArrayList<>(warnings) : new ArrayList<>();
        this.errorMessage = null;
    }
    
    /**
     * Constructor for failed analysis result.
     */
    protected AnalysisResult(String requestId, AnalysisType analysisType,
                           LocalDateTime startTime, LocalDateTime endTime,
                           String errorMessage) {
        this.requestId = requestId;
        this.analysisType = analysisType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = AnalysisStatus.FAILED;
        this.metadata = new HashMap<>();
        this.warnings = new ArrayList<>();
        this.errorMessage = errorMessage;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public AnalysisType getAnalysisType() { return analysisType; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public AnalysisStatus getStatus() { return status; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<String> getWarnings() { return warnings; }
    public String getErrorMessage() { return errorMessage; }
    
    /**
     * Duration of the analysis in milliseconds.
     */
    public long getDurationMs() {
        return java.time.Duration.between(startTime, endTime).toMillis();
    }
    
    /**
     * Whether the analysis was successful (SUCCESS or PARTIAL status).
     */
    public boolean isSuccessful() {
        return status == AnalysisStatus.SUCCESS || status == AnalysisStatus.PARTIAL;
    }
    
    /**
     * Get a summary of the analysis results.
     * Each concrete implementation should provide meaningful summary information.
     */
    public abstract String getSummary();
    
    /**
     * Get the total number of findings/issues discovered.
     * Used for quick overview and progress reporting.
     */
    public abstract int getTotalFindings();
    
    @Override
    public String toString() {
        return String.format("%s[id=%s, type=%s, status=%s, duration=%dms, findings=%d]",
            getClass().getSimpleName(), requestId, analysisType, status, 
            getDurationMs(), getTotalFindings());
    }
}
