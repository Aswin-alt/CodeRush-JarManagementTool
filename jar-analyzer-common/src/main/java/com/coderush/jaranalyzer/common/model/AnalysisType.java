package com.coderush.jaranalyzer.common.model;

/**
 * Enumeration of supported analysis types.
 * Each type corresponds to one of the six main features.
 */
public enum AnalysisType {
    /**
     * Feature 1: JAR Upgrade Impact Analysis
     * Analyzes the impact of upgrading a JAR dependency on project code.
     */
    JAR_UPGRADE_IMPACT("jar-upgrade-impact", "JAR Upgrade Impact Analysis"),
    
    /**
     * Feature 2: Unused and Duplicate JAR Detection
     * Identifies unused dependencies and duplicate JARs in the project.
     */
    UNUSED_JARS("unused-jars", "Unused and Duplicate JAR Detection"),
    
    /**
     * Feature 3: Deprecated API Usage Identification
     * Finds usage of deprecated methods and APIs in the project.
     */
    DEPRECATED_APIS("deprecated-apis", "Deprecated API Usage Identification"),
    
    /**
     * Feature 4: JAR Comparison
     * Compares two JAR versions and provides detailed changesets.
     */
    JAR_COMPARISON("jar-comparison", "JAR Version Comparison"),
    
    /**
     * Feature 5: Vulnerability Identification
     * Scans for security vulnerabilities using OSV-Scanner.
     */
    VULNERABILITY_SCAN("vulnerability-scan", "Vulnerability Identification"),
    
    /**
     * Feature 6: Ant Changes for JAR Upgrade (Future)
     * Generates Ant build script changes for JAR upgrades.
     */
    ANT_CHANGES("ant-changes", "Ant Changes for JAR Upgrade"),
    
    /**
     * Utility: Project JAR Scanning
     * Discovers and validates JAR files in project directories.
     */
    PROJECT_SCAN("project-scan", "Project JAR Scanning");
    
    private final String code;
    private final String displayName;
    
    AnalysisType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Get AnalysisType by code for API and CLI usage.
     */
    public static AnalysisType fromCode(String code) {
        for (AnalysisType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown analysis type code: " + code);
    }
}
