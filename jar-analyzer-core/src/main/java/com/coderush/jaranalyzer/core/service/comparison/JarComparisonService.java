package com.coderush.jaranalyzer.core.service.comparison;

import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonRequest;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonResult;

/**
 * Service interface for JAR comparison operations.
 * 
 * This service handles the core logic for comparing two JAR files
 * and identifying all changes at class, method, and field level.
 * 
 * Why this interface:
 * - Defines clear contract for JAR comparison
 * - Allows multiple implementations (ASM-based, reflection-based, etc.)
 * - Enables easy testing with mock implementations
 * - Provides consistency across different comparison strategies
 * - Supports dependency injection and service composition
 * 
 * Implementation Strategy:
 * - Use ASM for bytecode-level analysis (primary implementation)
 * - Extract class metadata and method signatures
 * - Compare old vs new JAR systematically
 * - Generate detailed change reports with compatibility impact analysis
 */
public interface JarComparisonService {
    
    /**
     * Compare two JAR files and generate a detailed change report.
     * 
     * This method performs comprehensive analysis including:
     * - Class-level changes (added, removed, modified classes)
     * - Method-level changes (signatures, parameters, return types, access modifiers)
     * - Field-level changes (types, access modifiers)
     * - Annotation changes (if enabled in request)
     * - Binary compatibility impact assessment
     * 
     * @param request The comparison request containing JAR files and analysis options
     * @return Detailed comparison result with all identified changes
     * @throws AnalysisException if comparison fails due to invalid JARs, I/O errors, etc.
     */
    JarComparisonResult compareJars(JarComparisonRequest request) throws AnalysisException;
    
    /**
     * Validate that the given JAR files can be compared.
     * 
     * This method performs preliminary checks:
     * - File existence and readability
     * - Valid JAR format
     * - Non-empty JAR contents
     * - ASM compatibility (bytecode version support)
     * 
     * @param request The comparison request to validate
     * @throws AnalysisException if validation fails
     */
    void validateComparisonRequest(JarComparisonRequest request) throws AnalysisException;
    
    /**
     * Get service metadata including supported features and limitations.
     * 
     * @return Service capability information
     */
    default ComparisonServiceMetadata getServiceMetadata() {
        return new ComparisonServiceMetadata(
            "ASM-based JAR Comparison Service",
            "1.0.0",
            "Performs detailed bytecode-level comparison using ASM library",
            true,  // supports method analysis
            true,  // supports field analysis
            true,  // supports annotation analysis
            true   // supports compatibility analysis
        );
    }
    
    /**
     * Metadata about the comparison service capabilities
     */
    class ComparisonServiceMetadata {
        private final String name;
        private final String version;
        private final String description;
        private final boolean supportsMethodAnalysis;
        private final boolean supportsFieldAnalysis;
        private final boolean supportsAnnotationAnalysis;
        private final boolean supportsCompatibilityAnalysis;
        
        public ComparisonServiceMetadata(String name, String version, String description,
                                       boolean supportsMethodAnalysis, boolean supportsFieldAnalysis,
                                       boolean supportsAnnotationAnalysis, boolean supportsCompatibilityAnalysis) {
            this.name = name;
            this.version = version;
            this.description = description;
            this.supportsMethodAnalysis = supportsMethodAnalysis;
            this.supportsFieldAnalysis = supportsFieldAnalysis;
            this.supportsAnnotationAnalysis = supportsAnnotationAnalysis;
            this.supportsCompatibilityAnalysis = supportsCompatibilityAnalysis;
        }
        
        // Getters
        public String getName() { return name; }
        public String getVersion() { return version; }
        public String getDescription() { return description; }
        public boolean supportsMethodAnalysis() { return supportsMethodAnalysis; }
        public boolean supportsFieldAnalysis() { return supportsFieldAnalysis; }
        public boolean supportsAnnotationAnalysis() { return supportsAnnotationAnalysis; }
        public boolean supportsCompatibilityAnalysis() { return supportsCompatibilityAnalysis; }
    }
}
