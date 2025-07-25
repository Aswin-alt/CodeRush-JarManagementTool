package com.coderush.jaranalyzer.common.model;

/**
 * Enumeration representing the status of an analysis request.
 * 
 * This enum defines the lifecycle states of any analysis operation,
 * providing a consistent status model across all analysis types.
 * 
 * Why this enum is in the common module:
 * - Shared across servlet, service, and other layers
 * - Prevents code duplication
 * - Ensures consistent status representation
 * - Easy to extend with new states if needed
 */
public enum AnalysisStatus {
    /**
     * Analysis request has been received and queued for processing.
     */
    QUEUED("Request received and queued for processing"),
    
    /**
     * Analysis is currently executing.
     */
    RUNNING("Analysis is currently executing"),
    
    /**
     * Analysis completed successfully.
     */
    COMPLETED("Analysis completed successfully"),
    
    /**
     * Analysis failed with an error.
     */
    FAILED("Analysis failed with an error"),
    
    /**
     * Analysis was cancelled by user request.
     */
    CANCELLED("Analysis was cancelled by user request");
    
    private final String description;
    
    AnalysisStatus(String description) {
        this.description = description;
    }
    
    /**
     * Get human-readable description of the status.
     * 
     * @return Status description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if the analysis is in a terminal state (completed, failed, or cancelled).
     * 
     * @return true if the analysis has finished (successfully or not)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
    
    /**
     * Check if the analysis is still active (queued or running).
     * 
     * @return true if the analysis is still in progress
     */
    public boolean isActive() {
        return this == QUEUED || this == RUNNING;
    }
}
