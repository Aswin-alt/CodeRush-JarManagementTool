package com.coderush.jaranalyzer.core.service.analyzer;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.exception.AnalysisException;

import java.nio.file.Path;
import java.util.Map;

/**
 * Service interface for Jdeprscan-based deprecated API detection.
 * 
 * Jdeprscan is a JDK tool specifically designed to find usage of
 * deprecated APIs in Java applications. This service provides:
 * - Deprecated API detection in JAR files
 * - JDK version compatibility checking
 * - Migration path recommendations
 * - Detailed usage reporting
 * 
 * Why Jdeprscan is essential for JAR upgrade analysis:
 * 1. Official JDK tool - comprehensive deprecated API database
 * 2. Covers all JDK APIs across versions
 * 3. Provides specific deprecation details (since version, forRemoval flag)
 * 4. Essential for Java version migration planning
 * 5. Lightweight and fast execution
 * 
 * This service enables:
 * - Proactive identification of deprecated API usage
 * - Assessment of upgrade risks before JAR updates
 * - Generation of migration reports for development teams
 * - Automated deprecated API monitoring in CI/CD pipelines
 */
public interface JdeprscanAnalyzerService {
    
    /**
     * Scan JAR file for deprecated API usage.
     * 
     * Executes jdeprscan to identify all usage of deprecated JDK APIs
     * within the specified JAR file.
     * 
     * @param jarPath Path to the JAR file to scan
     * @param options Scan options (JDK version, verbosity, output format)
     * @return Analysis result containing deprecated API usage details
     * @throws AnalysisException if scan fails or JAR cannot be processed
     */
    AnalysisResult scanForDeprecatedApis(Path jarPath, Map<String, Object> options) throws AnalysisException;
    
    /**
     * Scan project directory for deprecated API usage.
     * 
     * Recursively scans all JAR files and class files in a project
     * directory to build a comprehensive deprecated API usage report.
     * 
     * @param projectPath Path to the project root directory
     * @param options Scan options
     * @return Comprehensive project-wide deprecated API analysis
     * @throws AnalysisException if scan fails
     */
    AnalysisResult scanProjectForDeprecatedApis(Path projectPath, Map<String, Object> options) throws AnalysisException;
    
    /**
     * Compare deprecated API usage between two JAR versions.
     * 
     * Analyzes how deprecated API usage changes when upgrading from
     * one JAR version to another, helping assess migration effort.
     * 
     * @param oldJarPath Path to the older JAR version
     * @param newJarPath Path to the newer JAR version
     * @param options Comparison options
     * @return Analysis showing deprecated API usage changes
     * @throws AnalysisException if comparison fails
     */
    AnalysisResult compareDeprecatedApiUsage(Path oldJarPath, Path newJarPath,
                                           Map<String, Object> options) throws AnalysisException;
    
    /**
     * Generate migration recommendations for deprecated APIs.
     * 
     * Provides specific recommendations for replacing deprecated APIs
     * with current alternatives, including code examples where possible.
     * 
     * @param jarPath Path to the JAR file to analyze
     * @param targetJdkVersion Target JDK version for migration
     * @param options Generation options
     * @return Migration recommendations and alternatives
     * @throws AnalysisException if analysis fails
     */
    AnalysisResult generateMigrationRecommendations(Path jarPath, String targetJdkVersion,
                                                  Map<String, Object> options) throws AnalysisException;
    
    /**
     * Check if jdeprscan tool is available and functional.
     * 
     * @return true if jdeprscan is available in the current JDK
     */
    boolean isJdeprscanAvailable();
    
    /**
     * Get the version of jdeprscan tool being used.
     * 
     * @return jdeprscan version information
     */
    String getJdeprscanVersion();
    
    /**
     * Get list of JDK versions supported by the current jdeprscan installation.
     * 
     * @return Array of supported JDK version strings
     */
    String[] getSupportedJdkVersions();
}
