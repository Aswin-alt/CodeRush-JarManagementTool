package com.coderush.jaranalyzer.core.service;

import com.coderush.jaranalyzer.common.model.AnalysisRequest;
import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.model.AnalysisType;
import com.coderush.jaranalyzer.common.model.AnalysisStatusInfo;
import com.coderush.jaranalyzer.common.exception.AnalysisException;

import java.util.concurrent.CompletableFuture;

/**
 * Main service interface for JAR analysis operations.
 * 
 * This interface defines the contract for all JAR analysis functionality,
 * providing a unified API that abstracts the complexity of different analysis tools.
 * 
 * Why this interface design:
 * - Single entry point for all analysis operations
 * - Asynchronous support for long-running analysis tasks
 * - Type-safe analysis request/result handling
 * - Consistent error handling across all analysis types
 * - Easy to mock for testing
 * - Supports future extensibility for new analysis types
 * 
 * Benefits of this service layer:
 * 1. Modularity: Each analysis tool is encapsulated in its own service
 * 2. Testability: Interface allows easy mocking without real tool dependencies
 * 3. Maintainability: Changes to analysis logic don't affect the servlet layer
 * 4. Scalability: Services can be optimized and scaled independently
 * 5. Error isolation: Tool-specific failures are contained and handled gracefully
 */
public interface JarAnalysisService {
    
    /**
     * Execute analysis based on the provided request.
     * 
     * This method routes the request to the appropriate analysis tool service
     * based on the analysis type specified in the request.
     * 
     * @param request The analysis request containing type, files, and options
     * @return CompletableFuture containing the analysis result
     * @throws AnalysisException if the analysis cannot be started or fails
     */
    CompletableFuture<AnalysisResult> executeAnalysis(AnalysisRequest request) throws AnalysisException;
    
    /**
     * Check if a specific analysis type is supported and available.
     * 
     * This method verifies that the required tools and dependencies are
     * available for the specified analysis type.
     * 
     * @param analysisType The type of analysis to check
     * @return true if the analysis type is supported and tools are available
     */
    boolean isAnalysisSupported(AnalysisType analysisType);
    
    /**
     * Get the current status of a running analysis.
     * 
     * @param requestId The unique identifier of the analysis request
     * @return Current status information, or null if request not found
     */
    AnalysisStatusInfo getAnalysisStatus(String requestId);
    
    /**
     * Cancel a running analysis.
     * 
     * @param requestId The unique identifier of the analysis request
     * @return true if the analysis was successfully cancelled
     */
    boolean cancelAnalysis(String requestId);
    
    /**
     * Get analysis result by request ID.
     * 
     * @param requestId The unique identifier of the analysis request
     * @return The analysis result if available, null otherwise
     */
    AnalysisResult getAnalysisResult(String requestId);
}
