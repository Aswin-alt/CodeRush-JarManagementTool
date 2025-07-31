package com.coderush.jaranalyzer.common.model.upgrade;

import com.coderush.jaranalyzer.common.model.AnalysisRequest;
import com.coderush.jaranalyzer.common.model.AnalysisType;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Request model for JAR Upgrade Impact Analysis (Feature 1).
 * 
 * This analysis identifies all classes and methods in a project that will be
 * affected by upgrading a JAR dependency from one version to another.
 * 
 * Input Requirements:
 * - Project ZIP file containing source code and dependencies
 * - Old JAR file (current version being used)
 * - New JAR file (target version for upgrade)
 */
public class JarUpgradeRequest extends AnalysisRequest {
    
    private final File projectZipFile;
    private final File oldJarFile;
    private final File newJarFile;
    private final boolean includeTransitiveDependencies;
    private final boolean analyzeSourceCode;
    private final boolean analyzeBytecode;
    
    /**
     * Creates a JAR upgrade impact analysis request.
     * 
     * @param projectZipFile The project ZIP file to analyze
     * @param oldJarFile The current JAR version
     * @param newJarFile The target JAR version
     * @param metadata Additional analysis configuration
     */
    public JarUpgradeRequest(File projectZipFile, File oldJarFile, File newJarFile,
                           Map<String, Object> metadata) {
        super(AnalysisType.JAR_UPGRADE_IMPACT, metadata);
        this.projectZipFile = Objects.requireNonNull(projectZipFile, "Project ZIP file cannot be null");
        this.oldJarFile = Objects.requireNonNull(oldJarFile, "Old JAR file cannot be null");
        this.newJarFile = Objects.requireNonNull(newJarFile, "New JAR file cannot be null");
        
        // Extract configuration from metadata
        this.includeTransitiveDependencies = getBooleanMetadata("includeTransitiveDependencies", true);
        this.analyzeSourceCode = getBooleanMetadata("analyzeSourceCode", true);
        this.analyzeBytecode = getBooleanMetadata("analyzeBytecode", true);
    }
    
    /**
     * Convenience constructor with default metadata.
     */
    public JarUpgradeRequest(File projectZipFile, File oldJarFile, File newJarFile) {
        this(projectZipFile, oldJarFile, newJarFile, new HashMap<>());
    }
    
    private boolean getBooleanMetadata(String key, boolean defaultValue) {
        Object value = getMetadata().get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
    
    // Getters
    public File getProjectZipFile() { return projectZipFile; }
    public File getOldJarFile() { return oldJarFile; }
    public File getNewJarFile() { return newJarFile; }
    public boolean isIncludeTransitiveDependencies() { return includeTransitiveDependencies; }
    public boolean isAnalyzeSourceCode() { return analyzeSourceCode; }
    public boolean isAnalyzeBytecode() { return analyzeBytecode; }
    
    @Override
    public void validate() throws IllegalArgumentException {
        validateFile(projectZipFile, "Project ZIP file");
        validateFile(oldJarFile, "Old JAR file");
        validateFile(newJarFile, "New JAR file");
        
        // Validate file extensions
        if (!projectZipFile.getName().toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Project file must be a ZIP archive");
        }
        
        if (!oldJarFile.getName().toLowerCase().endsWith(".jar")) {
            throw new IllegalArgumentException("Old JAR file must have .jar extension");
        }
        
        if (!newJarFile.getName().toLowerCase().endsWith(".jar")) {
            throw new IllegalArgumentException("New JAR file must have .jar extension");
        }
        
        // Validate that old and new JARs are different
        if (oldJarFile.getAbsolutePath().equals(newJarFile.getAbsolutePath())) {
            throw new IllegalArgumentException("Old and new JAR files must be different");
        }
        
        // Validate at least one analysis type is enabled
        if (!analyzeSourceCode && !analyzeBytecode) {
            throw new IllegalArgumentException("At least one analysis type (source or bytecode) must be enabled");
        }
    }
    
    private void validateFile(File file, String description) {
        if (!file.exists()) {
            throw new IllegalArgumentException(description + " does not exist: " + file.getAbsolutePath());
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException(description + " is not a regular file: " + file.getAbsolutePath());
        }
        
        if (!file.canRead()) {
            throw new IllegalArgumentException(description + " is not readable: " + file.getAbsolutePath());
        }
        
        if (file.length() == 0) {
            throw new IllegalArgumentException(description + " is empty: " + file.getAbsolutePath());
        }
    }
    
    @Override
    public String getDescription() {
        return String.format("JAR Upgrade Impact Analysis: %s (%s -> %s) on project %s",
            extractJarName(oldJarFile),
            extractVersion(oldJarFile),
            extractVersion(newJarFile),
            projectZipFile.getName());
    }
    
    private String extractJarName(File jarFile) {
        String name = jarFile.getName();
        // Remove .jar extension and version numbers for cleaner display
        return name.replaceAll("\\.jar$", "").replaceAll("-\\d+.*$", "");
    }
    
    private String extractVersion(File jarFile) {
        String name = jarFile.getName();
        // Try to extract version from filename (simple heuristic)
        String[] parts = name.replaceAll("\\.jar$", "").split("-");
        for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i].matches("\\d+.*")) {
                return parts[i];
            }
        }
        return "unknown";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JarUpgradeRequest)) return false;
        JarUpgradeRequest that = (JarUpgradeRequest) o;
        return Objects.equals(projectZipFile, that.projectZipFile) &&
               Objects.equals(oldJarFile, that.oldJarFile) &&
               Objects.equals(newJarFile, that.newJarFile);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(projectZipFile, oldJarFile, newJarFile);
    }
}
