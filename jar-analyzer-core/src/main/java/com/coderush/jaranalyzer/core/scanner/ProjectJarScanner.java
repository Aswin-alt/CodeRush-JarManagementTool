package com.coderush.jaranalyzer.core.scanner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.coderush.jaranalyzer.common.model.AnalysisType;

/**
 * Project scanner utility for discovering JAR files and project structure.
 * 
 * The scanner package serves a specific purpose:
 * - Discovers JAR files in project directories
 * - Validates project structure and dependencies
 * - Provides file system utilities for analyzers
 * - Handles batch processing of multiple JAR files
 * 
 * This is DIFFERENT from analyzers and services:
 * - Analyzers: Implement business logic using multiple tools
 * - Services: Provide specific tool integrations
 * - Scanners: Handle file discovery and project structure
 * 
 * This separation allows:  
 * - Reusable file discovery logic across analyzers
 * - Consistent project structure validation
 * - Batch processing capabilities
 * - Separation of file system concerns from analysis logic
 */
public class ProjectJarScanner {
    
    private static final Logger logger = LoggerFactory.getLogger(ProjectJarScanner.class);
    
    // Common JAR file extensions
    private static final String[] JAR_EXTENSIONS = {".jar", ".war", ".ear"};
    
    // Directories commonly containing JAR files
    private static final String[] COMMON_JAR_DIRECTORIES = {
        "lib", "libs", "target", "build", "dist", "WEB-INF/lib", "META-INF/lib"
    };
    
    /**
     * Scans a project directory for JAR files.
     * 
     * @param projectPath Root path of the project to scan
     * @param recursive Whether to scan subdirectories recursively
     * @return List of discovered JAR file paths
     * @throws AnalysisException if scanning fails
     */
    public List<Path> scanForJarFiles(Path projectPath, boolean recursive) throws AnalysisException {
        logger.info("Scanning for JAR files in: {} (recursive: {})", projectPath, recursive);
        
        if (!Files.exists(projectPath)) {
            throw new AnalysisException(AnalysisType.PROJECT_SCAN, "scan-" + System.currentTimeMillis(),
                AnalysisException.ErrorCode.INPUT_NOT_FOUND,
                "Project path does not exist: " + projectPath, null);
        }
        
        if (!Files.isDirectory(projectPath)) {
            throw new AnalysisException(AnalysisType.PROJECT_SCAN, "scan-" + System.currentTimeMillis(),
                AnalysisException.ErrorCode.INVALID_INPUT,
                "Project path is not a directory: " + projectPath, null);
        }
        
        try {
            Stream<Path> pathStream = recursive 
                ? Files.walk(projectPath)
                : Files.list(projectPath);
                
            List<Path> jarFiles = pathStream
                .filter(Files::isRegularFile)
                .filter(this::isJarFile)
                .sorted()
                .collect(Collectors.toList());
                
            logger.info("Found {} JAR files in project: {}", jarFiles.size(), projectPath);
            
            if (logger.isDebugEnabled()) {
                jarFiles.forEach(jar -> logger.debug("  Found JAR: {}", jar));
            }
            
            return jarFiles;
            
        } catch (Exception e) {
            throw new AnalysisException(AnalysisType.PROJECT_SCAN, "scan-" + System.currentTimeMillis(),
                AnalysisException.ErrorCode.IO_ERROR,
                "Failed to scan project for JAR files: " + e.getMessage(), e);
        }
    }
    
    /**
     * Scans common JAR directories within a project.
     * 
     * This method focuses on directories commonly containing JAR files,
     * making it faster for typical project structures.
     * 
     * @param projectPath Root path of the project
     * @return List of JAR files found in common directories
     * @throws AnalysisException if scanning fails
     */
    public List<Path> scanCommonJarDirectories(Path projectPath) throws AnalysisException {
        logger.info("Scanning common JAR directories in: {}", projectPath);
        
        return Arrays.stream(COMMON_JAR_DIRECTORIES)
            .map(projectPath::resolve)
            .filter(Files::exists)
            .filter(Files::isDirectory)
            .flatMap(dir -> {
                try {
                    logger.debug("Scanning directory: {}", dir);
                    return Files.list(dir)
                        .filter(Files::isRegularFile)
                        .filter(this::isJarFile);
                } catch (Exception e) {
                    logger.warn("Failed to scan directory: " + dir, e);
                    return Stream.empty();
                }
            })
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Validates that a path contains a valid project structure.
     * 
     * @param projectPath Path to validate
     * @return true if the path appears to be a valid project
     */
    public boolean isValidProject(Path projectPath) {
        if (!Files.exists(projectPath) || !Files.isDirectory(projectPath)) {
            return false;
        }
        
        // Check for common project indicators
        return Arrays.stream(new String[]{"pom.xml", "build.gradle", "build.xml", "Makefile", "package.json"})
            .anyMatch(indicator -> Files.exists(projectPath.resolve(indicator)))
            || hasCommonProjectStructure(projectPath);
    }
    
    /**
     * Estimates the complexity of a project based on its structure.
     * 
     * @param projectPath Path to analyze
     * @return Complexity score (higher = more complex)
     */
    public int estimateProjectComplexity(Path projectPath) {
        try {
            int jarCount = scanForJarFiles(projectPath, true).size();
            int directoryDepth = calculateMaxDepth(projectPath);
            
            // Simple complexity heuristic
            return jarCount * 2 + directoryDepth;
            
        } catch (AnalysisException e) {
            logger.warn("Failed to estimate project complexity for: " + projectPath, e);
            return 1; // Default to low complexity
        }
    }
    
    /**
     * Checks if a file is a JAR file based on its extension.
     * 
     * @param path Path to check
     * @return true if the file appears to be a JAR file
     */
    private boolean isJarFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return Arrays.stream(JAR_EXTENSIONS)
            .anyMatch(fileName::endsWith);
    }
    
    /**
     * Checks if a directory has common project structure patterns.
     * 
     * @param projectPath Path to check
     * @return true if common project patterns are found
     */
    private boolean hasCommonProjectStructure(Path projectPath) {
        // Check for common directory patterns
        String[] commonDirs = {"src", "lib", "target", "build", "bin"};
        
        return Arrays.stream(commonDirs)
            .anyMatch(dir -> Files.exists(projectPath.resolve(dir)) 
                           && Files.isDirectory(projectPath.resolve(dir)));
    }
    
    /**
     * Calculates the maximum directory depth in a project.
     * 
     * @param projectPath Root path
     * @return Maximum depth of subdirectories
     */
    private int calculateMaxDepth(Path projectPath) {
        try (Stream<Path> paths = Files.walk(projectPath)) {
            return paths
                .filter(Files::isDirectory)
                .mapToInt(path -> path.getNameCount() - projectPath.getNameCount())
                .max()
                .orElse(0);
        } catch (Exception e) {
            logger.warn("Failed to calculate project depth for: " + projectPath, e);
            return 1;
        }
    }
}
