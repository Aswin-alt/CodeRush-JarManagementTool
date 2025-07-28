package com.coderush.jaranalyzer.core.servlet;

import com.coderush.jaranalyzer.common.model.AnalysisType;
import com.coderush.jaranalyzer.common.model.AnalysisStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.coderush.jaranalyzer.common.model.AnalysisResult;

/**
 * Context object to track analysis request state
 * 
 * This class maintains the state of an analysis request throughout its lifecycle:
 * - Request metadata (ID, type, parameters)
 * - Execution state (status, start time, error info)
 * - Future reference for cancellation support
 */
public class AnalysisRequestContext {
    private final String requestId;
    private final AnalysisType analysisType;
    private final Map<String, Object> requestData;
    private final LocalDateTime startTime;
    
    private AnalysisStatus status;
    private String error;
    private CompletableFuture<AnalysisResult> analysisFuture;
    
    public AnalysisRequestContext(String requestId, AnalysisType analysisType, 
                                Map<String, Object> requestData, LocalDateTime startTime) {
        this.requestId = requestId;
        this.analysisType = analysisType;
        this.requestData = requestData;
        this.startTime = startTime;
        this.status = AnalysisStatus.QUEUED;
    }
    
    // Getters
    public String getRequestId() { return requestId; }
    public AnalysisType getAnalysisType() { return analysisType; }
    public Map<String, Object> getRequestData() { return requestData; }
    public LocalDateTime getStartTime() { return startTime; }
    public AnalysisStatus getStatus() { return status; }
    public String getError() { return error; }
    public CompletableFuture<AnalysisResult> getAnalysisFuture() { return analysisFuture; }
    
    // Setters
    public void setStatus(AnalysisStatus status) { this.status = status; }
    public void setError(String error) { this.error = error; }
    public void setAnalysisFuture(CompletableFuture<AnalysisResult> analysisFuture) { 
        this.analysisFuture = analysisFuture; 
    }
}
