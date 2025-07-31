package com.coderush.jaranalyzer.core.analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coderush.jaranalyzer.common.analyzer.AnalyzerMetadata;
import com.coderush.jaranalyzer.common.analyzer.ProjectAnalyzer;
import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.coderush.jaranalyzer.common.model.AnalysisType;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonRequest;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonResult;
import com.coderush.jaranalyzer.core.service.comparison.JarComparisonService;

/**
 * Concrete implementation of JAR comparison analysis (Feature 1).
 * 
 * This analyzer performs comprehensive comparison between two JAR versions
 * to identify all changes at class and method level using ASM bytecode analysis.
 * 
 * Why this architecture for JAR comparison:
 * - Type Safety: Implements ProjectAnalyzer<JarComparisonRequest, JarComparisonResult>
 * - Service Delegation: Uses JarComparisonService for actual comparison logic
 * - Single Responsibility: Focuses on orchestrating the comparison workflow
 * - Testability: Can mock JarComparisonService for testing
 * - Maintainability: Clear separation between analyzer and service layers
 * 
 * Analysis Process:
 * 1. Validate input JAR files and configuration
 * 2. Delegate to JarComparisonService for ASM-based analysis
 * 3. Process and enrich results with additional metadata
 * 4. Return comprehensive comparison report
 * 
 * This analyzer serves as the entry point for JAR comparison analysis,
 * providing a consistent interface that integrates with the servlet layer.
 */
public class JarComparisonAnalyzer implements ProjectAnalyzer<JarComparisonRequest, JarComparisonResult> {
    
    private static final Logger logger = LoggerFactory.getLogger(JarComparisonAnalyzer.class);
    
    // Service dependency - injected via constructor
    private final JarComparisonService jarComparisonService;
    
    /**
     * Constructor with service injection.
     * 
     * @param jarComparisonService The service to perform actual JAR comparison
     */
    public JarComparisonAnalyzer(JarComparisonService jarComparisonService) {
        this.jarComparisonService = jarComparisonService;
        logger.info("JarComparisonAnalyzer initialized with service: {}", 
            jarComparisonService.getClass().getSimpleName());
    }
    
    /**
     * Performs comprehensive JAR comparison analysis.
     * 
     * This method orchestrates the entire comparison workflow:
     * 1. Validates the comparison request
     * 2. Delegates to the comparison service
     * 3. Returns comprehensive analysis results
     */
    @Override
    public JarComparisonResult analyze(JarComparisonRequest request) throws AnalysisException {
        logger.info("Starting JAR comparison analysis: {}", request.getDescription());
        logger.debug("Request details: old={}, new={}, includePrivate={}, analyzeFields={}", 
            request.getOldJarFile().getName(), 
            request.getNewJarFile().getName(),
            request.isIncludePrivateMembers(),
            request.isAnalyzeFieldChanges());
        
        try {
            // Delegate the actual comparison to the service
            JarComparisonResult result = jarComparisonService.compareJars(request);
            
            // Log additional context for the analysis
            logAnalysisGuidance(result, request);
            
            logger.info("JAR comparison analysis completed successfully. Found {} changes", 
                result.getChanges().size());
            logger.debug("Analysis summary: {} classes in old JAR, {} classes in new JAR", 
                result.getOldJarClassCount(), result.getNewJarClassCount());
            
            return result;
            
        } catch (AnalysisException e) {
            logger.error("JAR comparison analysis failed: {}", e.getMessage(), e);
            throw e; // Re-throw as-is
        } catch (Exception e) {
            logger.error("Unexpected error during JAR comparison analysis: {}", e.getMessage(), e);
            throw new AnalysisException(AnalysisType.JAR_COMPARISON,
                "Unexpected error during analysis: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates that the analyzer can handle this request.
     * 
     * This method ensures the request is compatible with this analyzer
     * and that all required dependencies are available.
     */
    public boolean canAnalyze(JarComparisonRequest request) {
        if (request == null) {
            logger.warn("Cannot analyze null request");
            return false;
        }
        
        if (!AnalysisType.JAR_COMPARISON.equals(request.getAnalysisType())) {
            logger.warn("Cannot analyze request of type: {}", request.getAnalysisType());
            return false;
        }
        
        try {
            // Use the service to validate the request
            jarComparisonService.validateComparisonRequest(request);
            logger.debug("Request validation successful for: {}", request.getRequestId());
            return true;
            
        } catch (Exception e) {
            logger.warn("Request validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Checks if this analyzer supports the given analysis type.
     */
    @Override
    public boolean supports(AnalysisType type) {
        return AnalysisType.JAR_COMPARISON.equals(type);
    }
    
    /**
     * Returns the analysis type this analyzer handles.
     */
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.JAR_COMPARISON;
    }
    
    /**
     * Validates that the given request can be processed by this analyzer.
     */
    @Override
    public void validateRequest(JarComparisonRequest request) throws IllegalArgumentException {
        try {
            jarComparisonService.validateComparisonRequest(request);
        } catch (Exception e) {
            throw new IllegalArgumentException("Request validation failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Estimates the time required for analysis based on input size.
     */
    @Override
    public long estimateDuration(JarComparisonRequest request) {
        // Base time for setup and initialization
        long baseTime = 5000; // 5 seconds
        
        // Estimate based on file sizes
        long oldJarSize = request.getOldJarFile().length();
        long newJarSize = request.getNewJarFile().length();
        long totalSize = oldJarSize + newJarSize;
        
        // Rough estimate: 1MB takes about 2 seconds to analyze
        long sizeBasedTime = (totalSize / (1024 * 1024)) * 2000;
        
        // Additional time for complex analysis options
        long optionsTime = 0;
        if (request.isIncludePrivateMembers()) optionsTime += 1000;
        if (request.isAnalyzeFieldChanges()) optionsTime += 1000;
        if (request.isAnalyzeAnnotations()) optionsTime += 2000;
        
        long estimatedTime = baseTime + sizeBasedTime + optionsTime;
        
        logger.debug("Estimated analysis duration: {}ms for JAR sizes {}MB + {}MB", 
            estimatedTime, oldJarSize / (1024 * 1024), newJarSize / (1024 * 1024));
        
        return estimatedTime;
    }
    
    /**
     * Returns metadata about this analyzer's capabilities.
     * 
     * This information helps the framework understand what this analyzer
     * can do and allows for dynamic capability discovery.
     */
    @Override
    public AnalyzerMetadata getMetadata() {
        return new AnalyzerMetadata(
            "JAR Comparison Analyzer",                    // name
            "Compares two JAR files using ASM bytecode analysis", // description
            "1.0.0",                                     // version
            new String[]{"ASM", "Java 11+"},            // requiredTools
            false,                                       // supportsProgressReporting
            false                                        // supportsCancellation
        );
    }
    
    /**
     * Logs analysis guidance for the user.
     * 
     * This method provides contextual information that helps users
     * understand the analysis results better.
     */
    private void logAnalysisGuidance(JarComparisonResult result, JarComparisonRequest request) {
        logger.debug("Providing analysis guidance");
        
        // Log analysis configuration
        if (request.isIncludePrivateMembers()) {
            logger.info("Analysis includes private members - internal changes will be reported");
        }
        
        if (!request.isAnalyzeFieldChanges()) {
            logger.info("Field-level analysis was disabled - field changes not reported");
        }
        
        if (!request.isAnalyzeAnnotations()) {
            logger.info("Annotation analysis was disabled - annotation changes not reported");
        }
        
        // Log compatibility guidance
        long breakingChanges = result.getChanges().stream()
            .mapToLong(change -> change.getCompatibilityImpact() == 
                com.coderush.jaranalyzer.common.model.comparison.ChangeDetail.CompatibilityImpact.BREAKING ? 1 : 0)
            .sum();
            
        if (breakingChanges > 0) {
            logger.warn("Found {} breaking changes that may affect binary compatibility", breakingChanges);
        } else {
            logger.info("No breaking changes detected - upgrade should be binary compatible");
        }
        
        logger.debug("Analysis guidance completed");
    }
}
