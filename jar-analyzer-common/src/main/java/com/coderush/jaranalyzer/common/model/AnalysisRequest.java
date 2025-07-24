package com.coderush.jaranalyzer.common.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Base class for all analysis requests.
 * Contains common properties and metadata for any analysis operation.
 * 
 * This abstract class ensures consistency across all six features and provides
 * extensibility for future analysis types.
 */
public abstract class AnalysisRequest {
    
    private final String requestId;
    private final AnalysisType analysisType;
    private final LocalDateTime timestamp;
    private final Map<String, Object> metadata;
    
    /**
     * Constructor for analysis request.
     * 
     * @param analysisType The type of analysis to perform
     * @param metadata Additional metadata for the analysis
     */
    protected AnalysisRequest(AnalysisType analysisType, Map<String, Object> metadata) {
        this.requestId = UUID.randomUUID().toString();
        this.analysisType = analysisType;
        this.timestamp = LocalDateTime.now();
        this.metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
    
    /**
     * Unique identifier for this analysis request.
     * Used for tracking and correlation across logs and UI.
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * The type of analysis being performed.
     */
    public AnalysisType getAnalysisType() {
        return analysisType;
    }
    
    /**
     * When this request was created.
     */
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    /**
     * Additional metadata for the analysis.
     * Can include user preferences, configuration options, etc.
     */
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    /**
     * Validates that the request has all required inputs.
     * Each concrete implementation should validate its specific requirements.
     * 
     * @throws IllegalArgumentException if validation fails
     */
    public abstract void validate() throws IllegalArgumentException;
    
    /**
     * Returns a human-readable description of this analysis request.
     */
    public abstract String getDescription();
    
    @Override
    public String toString() {
        return String.format("%s[id=%s, type=%s, timestamp=%s]", 
            getClass().getSimpleName(), requestId, analysisType, timestamp);
    }
}
