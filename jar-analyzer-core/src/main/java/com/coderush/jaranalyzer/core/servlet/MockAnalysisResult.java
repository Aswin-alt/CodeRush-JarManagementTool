package com.coderush.jaranalyzer.core.servlet;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.model.AnalysisType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Mock implementation of AnalysisResult for testing purposes
 * This will be replaced with real results from core analysis modules
 */
public class MockAnalysisResult extends AnalysisResult {
    private final String message;
    
    public MockAnalysisResult(String requestId, AnalysisType analysisType, 
                            String message, LocalDateTime completionTime) {
        super(requestId, analysisType, completionTime.minusSeconds(2), completionTime, 
              Map.of("mock", true), List.of());
        this.message = message;
    }
    
    @Override
    public String getSummary() {
        return message;
    }
    
    @Override
    public int getTotalFindings() {
        return 0; // Mock result has no findings
    }
    
    public String getMessage() { return message; }
}
