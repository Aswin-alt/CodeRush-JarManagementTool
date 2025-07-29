package com.coderush.jaranalyzer.common.model.comparison;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.coderush.jaranalyzer.common.model.AnalysisRequest;
import com.coderush.jaranalyzer.common.model.AnalysisType;

/**
 * Request model for JAR Comparison Analysis (Feature 1).
 * 
 * This analysis performs detailed comparison between two JAR versions
 * to identify all changes at class and method level using ASM bytecode analysis.
 * 
 * Input Requirements:
 * - Jar1 file (lower/older version) - baseline for comparison
 * - Jar2 file (higher/newer version) - target for comparison
 * 
 * Analysis Capabilities:
 * - Class-level changes (added, removed, modified classes)
 * - Method-level changes (signature changes, parameter changes, return type changes)
 * - Field-level changes (type changes, access modifier changes)
 * - Access modifier changes (public -> private, etc.)
 * - Annotation changes and metadata differences
 * 
 * Why this model design:
 * - Extends AnalysisRequest for consistent validation and metadata handling
 * - Immutable design prevents accidental modification during analysis
 * - Clear naming (oldJar vs newJar) indicates comparison direction
 * - Configuration options allow users to control analysis depth
 */
public class JarComparisonRequest extends AnalysisRequest {
    
    private final File oldJarFile;              // Lower version JAR (baseline)
    private final File newJarFile;              // Higher version JAR (comparison target)
    private final boolean includePrivateMembers;    // Include private methods/fields in analysis
    private final boolean includePackageClasses;    // Include package-private classes
    private final boolean analyzeFieldChanges;      // Analyze field-level changes
    private final boolean analyzeAnnotations;       // Analyze annotation changes
    private final boolean detectBinaryCompatibility; // Detect binary compatibility issues
    
    /**
     * Creates a JAR comparison analysis request.
     * 
     * @param oldJarFile The baseline JAR file (lower version)
     * @param newJarFile The target JAR file (higher version)
     * @param metadata Additional analysis configuration options
     */
    public JarComparisonRequest(File oldJarFile, File newJarFile, Map<String, Object> metadata) {
        super(AnalysisType.JAR_COMPARISON, metadata);
        
        this.oldJarFile = Objects.requireNonNull(oldJarFile, "Old JAR file cannot be null");
        this.newJarFile = Objects.requireNonNull(newJarFile, "New JAR file cannot be null");
        
        // Extract configuration from metadata with sensible defaults
        // These options allow users to control the depth and scope of analysis
        this.includePrivateMembers = getBooleanFromMetadata("includePrivateMembers", false);
        this.includePackageClasses = getBooleanFromMetadata("includePackageClasses", true);
        this.analyzeFieldChanges = getBooleanFromMetadata("analyzeFieldChanges", true);
        this.analyzeAnnotations = getBooleanFromMetadata("analyzeAnnotations", true);
        this.detectBinaryCompatibility = getBooleanFromMetadata("detectBinaryCompatibility", true);
    }
    
    /**
     * Helper method to extract boolean values from metadata.
     */
    private boolean getBooleanFromMetadata(String key, boolean defaultValue) {
        Object value = getMetadata().get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    /**
     * Convenience constructor with default options for simple comparisons.
     */
    public JarComparisonRequest(File oldJarFile, File newJarFile) {
        this(oldJarFile, newJarFile, new HashMap<String, Object>());
    }
    
    /**
     * Validates the request inputs.
     * 
     * Why validation is crucial:
     * - Prevents runtime failures during ASM analysis
     * - Provides clear error messages for troubleshooting
     * - Ensures files are accessible before starting expensive analysis
     * - Validates JAR format to avoid ASM parsing errors
     */
    @Override
    public void validate() throws IllegalArgumentException {
        // Validate basic request structure (would call super.validate() if not abstract)
        if (getRequestId() == null || getAnalysisType() == null) {
            throw new IllegalArgumentException("Request ID and analysis type are required");
        }
        
        // Check file existence
        if (!oldJarFile.exists()) {
            throw new IllegalArgumentException("Old JAR file does not exist: " + oldJarFile.getPath());
        }
        
        if (!newJarFile.exists()) {
            throw new IllegalArgumentException("New JAR file does not exist: " + newJarFile.getPath());
        }
        
        // Check file readability
        if (!oldJarFile.canRead()) {
            throw new IllegalArgumentException("Cannot read old JAR file: " + oldJarFile.getPath());
        }
        
        if (!newJarFile.canRead()) {
            throw new IllegalArgumentException("Cannot read new JAR file: " + newJarFile.getPath());
        }
        
        // Validate JAR format
        if (!isJarFile(oldJarFile)) {
            throw new IllegalArgumentException("Old file is not a JAR file: " + oldJarFile.getName());
        }
        
        if (!isJarFile(newJarFile)) {
            throw new IllegalArgumentException("New file is not a JAR file: " + newJarFile.getName());
        }
        
        // Prevent comparing the same file
        if (oldJarFile.equals(newJarFile)) {
            throw new IllegalArgumentException("Cannot compare JAR file with itself: " + oldJarFile.getName());
        }
    }
    
    /**
     * Checks if a file is a valid JAR/archive file.
     * This prevents ASM from trying to parse non-JAR files.
     */
    private boolean isJarFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".war") || name.endsWith(".ear") || name.endsWith(".zip");
    }
    
    @Override
    public String getDescription() {
        return String.format("Compare JAR files: %s vs %s", 
            oldJarFile.getName(), newJarFile.getName());
    }
    
    // Getters - immutable access to request properties
    public File getOldJarFile() { return oldJarFile; }
    public File getNewJarFile() { return newJarFile; }
    public boolean isIncludePrivateMembers() { return includePrivateMembers; }
    public boolean isIncludePackageClasses() { return includePackageClasses; }
    public boolean isAnalyzeFieldChanges() { return analyzeFieldChanges; }
    public boolean isAnalyzeAnnotations() { return analyzeAnnotations; }
    public boolean isDetectBinaryCompatibility() { return detectBinaryCompatibility; }
    
    @Override
    public String toString() {
        return String.format("JarComparisonRequest{oldJar='%s', newJar='%s', requestId='%s'}", 
            oldJarFile.getName(), newJarFile.getName(), getRequestId());
    }
}
