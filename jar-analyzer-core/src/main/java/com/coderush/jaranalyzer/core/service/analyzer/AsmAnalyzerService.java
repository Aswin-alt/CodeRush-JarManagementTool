package com.coderush.jaranalyzer.core.service.analyzer;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.exception.AnalysisException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service interface for ASM-based bytecode analysis.
 * 
 * ASM is a powerful Java bytecode manipulation framework that allows
 * deep inspection of compiled Java classes. This service provides:
 * - Method signature analysis
 * - Field access pattern detection
 * - Annotation processing
 * - Custom bytecode inspection
 * 
 * Why ASM is crucial for advanced JAR analysis:
 * 1. Direct bytecode access - more accurate than reflection
 * 2. Can analyze private/protected members
 * 3. Detects actual usage patterns vs. declared dependencies
 * 4. Supports advanced analysis like call graph generation
 * 5. Framework-agnostic - works with any Java bytecode
 * 
 * ASM enables sophisticated analysis features:
 * - Finding deprecated API usage in bytecode
 * - Detecting reflection-based dependency usage
 * - Analyzing annotation-based frameworks (Spring, etc.)
 * - Building detailed call graphs for impact analysis
 * - Identifying security-sensitive code patterns
 */
public interface AsmAnalyzerService {
    
    /**
     * Analyze bytecode of a JAR file for API usage patterns.
     * 
     * This method performs deep bytecode inspection to identify:
     * - Method calls to external APIs
     * - Field access patterns
     * - Annotation usage
     * - Reflection-based API calls
     * 
     * @param jarPath Path to the JAR file to analyze
     * @param targetApis List of API patterns to search for (optional)
     * @param options Analysis options (depth, filters, etc.)
     * @return Analysis result containing API usage information
     * @throws AnalysisException if bytecode cannot be read or analyzed
     */
    AnalysisResult analyzeApiUsage(Path jarPath, List<String> targetApis, 
                                 Map<String, Object> options) throws AnalysisException;
    
    /**
     * Find deprecated API usage in JAR bytecode.
     * 
     * Scans bytecode for calls to methods/classes marked with @Deprecated
     * annotation or known deprecated APIs from external libraries.
     * 
     * @param jarPath Path to the JAR file to analyze
     * @param deprecatedApiDatabase Database of known deprecated APIs
     * @param options Analysis options
     * @return Result containing deprecated API usage information
     * @throws AnalysisException if analysis fails
     */
    AnalysisResult findDeprecatedApiUsage(Path jarPath, Map<String, List<String>> deprecatedApiDatabase,
                                        Map<String, Object> options) throws AnalysisException;
    
    /**
     * Generate call graph for impact analysis.
     * 
     * Creates a detailed call graph showing method invocation relationships,
     * which is essential for understanding the impact of API changes.
     * 
     * @param jarPath Path to the JAR file to analyze
     * @param focusClasses List of classes to focus the analysis on (optional)
     * @param options Analysis options (depth, direction, filters)
     * @return Analysis result containing call graph information
     * @throws AnalysisException if analysis fails
     */
    AnalysisResult generateCallGraph(Path jarPath, List<String> focusClasses,
                                   Map<String, Object> options) throws AnalysisException;
    
    /**
     * Compare bytecode between two JAR versions.
     * 
     * Performs detailed comparison of class files to identify:
     * - Added/removed methods
     * - Signature changes
     * - Behavioral differences (when possible to detect)
     * 
     * @param oldJarPath Path to the older JAR version
     * @param newJarPath Path to the newer JAR version
     * @param options Comparison options
     * @return Detailed comparison result
     * @throws AnalysisException if comparison fails
     */
    AnalysisResult compareJarBytecode(Path oldJarPath, Path newJarPath,
                                    Map<String, Object> options) throws AnalysisException;
    
    /**
     * Extract metadata from JAR bytecode.
     * 
     * Collects useful metadata like:
     * - Java version compatibility
     * - Framework annotations
     * - Custom attributes
     * - Class hierarchy information
     * 
     * @param jarPath Path to the JAR file
     * @param options Extraction options
     * @return Metadata analysis result
     * @throws AnalysisException if extraction fails
     */
    AnalysisResult extractMetadata(Path jarPath, Map<String, Object> options) throws AnalysisException;
    
    /**
     * Check if ASM library is available and functional.
     * 
     * @return true if ASM can be used for analysis
     */
    boolean isAsmAvailable();
    
    /**
     * Get the version of the ASM library being used.
     * 
     * @return ASM version string
     */
    String getAsmVersion();
}
