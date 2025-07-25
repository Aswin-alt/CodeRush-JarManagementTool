package com.coderush.jaranalyzer.common.model;

import java.time.LocalDateTime;

/**
 * Comprehensive status information for analysis execution.
 * 
 * This class provides detailed status information beyond just the enum state,
 * including progress tracking, timing information, and contextual messages.
 * 
 * Why this class design:
 * - Combines the simple AnalysisStatus enum with rich contextual information
 * - Provides progress tracking for long-running analyses
 * - Includes timing information for performance monitoring
 * - Supports detailed error reporting
 * - Immutable design for thread safety
 * 
 * Benefits of separating enum from info class:
 * - AnalysisStatus enum remains simple and reusable
 * - AnalysisStatusInfo provides rich context when needed
 * - Clear separation of concerns
 * - Easy to serialize for API responses
 */
public class AnalysisStatusInfo {
    private final String requestId;
    private final AnalysisType analysisType;
    private final AnalysisStatus status;
    private final String message;
    private final double progressPercentage;
    private final LocalDateTime startTime;
    private final LocalDateTime lastUpdated;
    private final String errorDetails;
    
    /**
     * Constructor for general status information.
     */
    public AnalysisStatusInfo(String requestId, AnalysisType analysisType, 
                            AnalysisStatus status, String message, 
                            double progressPercentage, LocalDateTime startTime) {
        this(requestId, analysisType, status, message, progressPercentage, 
             startTime, LocalDateTime.now(), null);
    }
    
    /**
     * Constructor for failed status with error details.
     */
    public AnalysisStatusInfo(String requestId, AnalysisType analysisType,
                            AnalysisStatus status, String message,
                            double progressPercentage, LocalDateTime startTime,
                            String errorDetails) {
        this(requestId, analysisType, status, message, progressPercentage,
             startTime, LocalDateTime.now(), errorDetails);
    }
    
    /**
     * Full constructor with all information.
     */
    public AnalysisStatusInfo(String requestId, AnalysisType analysisType,
                            AnalysisStatus status, String message,
                            double progressPercentage, LocalDateTime startTime,
                            LocalDateTime lastUpdated, String errorDetails) {
        this.requestId = requestId;
        this.analysisType = analysisType;
        this.status = status;
        this.message = message;
        this.progressPercentage = Math.max(0.0, Math.min(100.0, progressPercentage));
        this.startTime = startTime;
        this.lastUpdated = lastUpdated;
        this.errorDetails = errorDetails;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public AnalysisType getAnalysisType() { return analysisType; }
    public AnalysisStatus getStatus() { return status; }
    public String getMessage() { return message; }
    public double getProgressPercentage() { return progressPercentage; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public String getErrorDetails() { return errorDetails; }
    
    /**
     * Check if this status represents a failed analysis.
     */
    public boolean isFailed() {
        return status == AnalysisStatus.FAILED;
    }
    
    /**
     * Check if this status represents a completed analysis.
     */
    public boolean isCompleted() {
        return status == AnalysisStatus.COMPLETED;
    }
    
    /**
     * Check if this status represents an active analysis.
     */
    public boolean isActive() {
        return status.isActive();
    }
    
    /**
     * Get elapsed time since analysis started.
     */
    public long getElapsedTimeMs() {
        return java.time.Duration.between(startTime, lastUpdated).toMillis();
    }
    
    /**
     * Create a new status info with updated progress.
     */
    public AnalysisStatusInfo withProgress(double newProgress, String newMessage) {
        return new AnalysisStatusInfo(requestId, analysisType, status, 
                                    newMessage, newProgress, startTime);
    }
    
    /**
     * Create a new status info with updated status.
     */
    public AnalysisStatusInfo withStatus(AnalysisStatus newStatus, String newMessage) {
        return new AnalysisStatusInfo(requestId, analysisType, newStatus,
                                    newMessage, progressPercentage, startTime);
    }
    
    @Override
    public String toString() {
        return String.format("AnalysisStatusInfo[id=%s, type=%s, status=%s, progress=%.1f%%, elapsed=%dms]",
                requestId, analysisType, status, progressPercentage, getElapsedTimeMs());
    }
}
