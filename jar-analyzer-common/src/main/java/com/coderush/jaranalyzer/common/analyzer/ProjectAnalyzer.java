package com.coderush.jaranalyzer.common.analyzer;

import com.coderush.jaranalyzer.common.model.*;
import com.coderush.jaranalyzer.common.exception.AnalysisException;

/**
 * Main interface for all JAR analysis operations.
 * 
 * This interface defines the contract that all six features must implement:
 * 1. JAR Upgrade Impact Analysis
 * 2. Unused & Duplicate JAR Detection  
 * 3. Deprecated API Usage Identification
 * 4. JAR Comparison
 * 5. Vulnerability Identification
 * 6. Ant Changes for JAR Upgrade (future)
 * 
 * The generic type parameters allow for type-safe implementation while
 * maintaining a consistent interface across all analysis types.
 * 
 * @param <T> The specific request type for this analyzer
 * @param <R> The specific result type for this analyzer
 */
public interface ProjectAnalyzer<T extends AnalysisRequest, R extends AnalysisResult> {
    
    /**
     * Performs the analysis operation.
     * 
     * This is the main entry point for all analysis operations. Each implementation
     * should handle its specific analysis logic while following these principles:
     * - Validate input thoroughly
     * - Provide progress updates for long-running operations
     * - Handle errors gracefully with meaningful error messages
     * - Return comprehensive results
     * 
     * @param request The analysis request containing all necessary inputs
     * @return The analysis result with findings and metadata
     * @throws AnalysisException if the analysis cannot be completed
     */
    R analyze(T request) throws AnalysisException;
    
    /**
     * Checks if this analyzer supports the given analysis type.
     * 
     * This allows for dynamic analyzer discovery and validation.
     * Each analyzer should return true only for its specific analysis type.
     * 
     * @param type The analysis type to check
     * @return true if this analyzer can handle the given type
     */
    boolean supports(AnalysisType type);
    
    /**
     * Returns the analysis type this analyzer handles.
     * 
     * Used for analyzer registration and discovery.
     * 
     * @return The specific AnalysisType this analyzer implements
     */
    AnalysisType getAnalysisType();
    
    /**
     * Validates that the given request can be processed by this analyzer.
     * 
     * This method should perform comprehensive validation of:
     * - Request type compatibility
     * - Required inputs presence and validity
     * - System prerequisites (tools, permissions, etc.)
     * 
     * @param request The request to validate
     * @throws IllegalArgumentException if validation fails
     */
    void validateRequest(T request) throws IllegalArgumentException;
    
    /**
     * Returns metadata about this analyzer's capabilities.
     * 
     * This information can be used for:
     * - UI feature discovery
     * - API documentation
     * - Performance estimation
     * 
     * @return Analyzer capability metadata
     */
    AnalyzerMetadata getMetadata();
    
    /**
     * Estimates the time required for analysis based on input size.
     * 
     * This helps with:
     * - UI progress estimation
     * - Resource planning
     * - Timeout configuration
     * 
     * @param request The analysis request
     * @return Estimated duration in milliseconds
     */
    long estimateDuration(T request);
}
