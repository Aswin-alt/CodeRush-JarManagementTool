package com.coderush.jaranalyzer.core.service.analyzer;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.exception.AnalysisException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service interface for JDeps-based dependency analysis.
 * 
 * JDeps is a Java command-line tool that analyzes class dependencies.
 * This service wraps JDeps functionality to provide:
 * - Package dependency analysis
 * - Module dependency analysis  
 * - API usage detection
 * - Classpath verification
 * 
 * Why JDeps is essential for JAR analysis:
 * 1. Built into JDK - no external dependencies
 * 2. Comprehensive dependency mapping
 * 3. Can detect unused dependencies
 * 4. Provides detailed package-level analysis
 * 5. Supports both legacy and modular Java applications
 * 
 * This interface allows us to:
 * - Mock JDeps for testing without actual tool execution
 * - Swap implementations (e.g., direct tool execution vs. API calls)
 * - Add caching, rate limiting, or other cross-cutting concerns
 * - Standardize error handling across different JDeps operations
 */
public interface JDepsAnalyzerService {
    
    /**
     * Analyze dependencies of a JAR file.
     * 
     * This method runs JDeps analysis on a single JAR file to discover:
     * - What packages this JAR depends on
     * - What JDK modules are required
     * - Internal vs. external dependencies
     * 
     * @param jarPath Path to the JAR file to analyze
     * @param options Analysis options (verbose level, output format, etc.)
     * @return Analysis result containing dependency information
     * @throws AnalysisException if JDeps execution fails or JAR is invalid
     */
    AnalysisResult analyzeDependencies(Path jarPath, Map<String, Object> options) throws AnalysisException;
    
    /**
     * Analyze dependencies across multiple JAR files in a project.
     * 
     * This is useful for detecting:
     * - Duplicate dependencies across JARs
     * - Conflicting versions
     * - Unused JARs in the classpath
     * 
     * @param jarPaths List of JAR file paths to analyze
     * @param classpathPaths Additional classpath entries
     * @param options Analysis options
     * @return Comprehensive dependency analysis result
     * @throws AnalysisException if analysis fails
     */
    AnalysisResult analyzeProjectDependencies(List<Path> jarPaths, List<Path> classpathPaths, 
                                            Map<String, Object> options) throws AnalysisException;
    
    /**
     * Find unused dependencies in a project.
     * 
     * Compares declared dependencies (from build files) with actual usage
     * (from bytecode analysis) to identify unused JARs.
     * 
     * @param projectPath Path to the project root
     * @param declaredDependencies List of declared dependency JAR paths
     * @param options Analysis options
     * @return Result containing unused dependency information
     * @throws AnalysisException if analysis fails
     */
    AnalysisResult findUnusedDependencies(Path projectPath, List<Path> declaredDependencies,
                                        Map<String, Object> options) throws AnalysisException;
    
    /**
     * Check if JDeps tool is available and functional.
     * 
     * @return true if JDeps is available and can be executed
     */
    boolean isJDepsAvailable();
    
    /**
     * Get the version of the JDeps tool being used.
     * 
     * @return JDeps version string, or null if not available
     */
    String getJDepsVersion();
}
