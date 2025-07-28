package com.coderush.jaranalyzer.common.model.upgrade;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.model.AnalysisType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Result model for JAR upgrade impact analysis.
 * 
 * This contains the comprehensive results of analyzing the impact
 * of upgrading from one JAR version to another, including all
 * tool-specific analysis results.
 */
public class JarUpgradeResult extends AnalysisResult {
    
    // Individual analysis results from different tools
    private Object dependencyAnalysis;    // From JDeps
    private Object apiUsageAnalysis;      // From ASM
    private Object deprecatedApiAnalysis; // From Jdeprscan
    private Object vulnerabilityAnalysis; // From OSV-Scanner
    
    /**
     * Constructor for successful JAR upgrade analysis result.
     * 
     * @param requestId Unique identifier matching the request
     * @param analysisType Type of analysis performed
     * @param startTime When analysis began
     * @param endTime When analysis completed
     * @param metadata Additional result metadata
     * @param warnings Any warnings generated during analysis
     */
    public JarUpgradeResult(String requestId, AnalysisType analysisType,
                          LocalDateTime startTime, LocalDateTime endTime,
                          Map<String, Object> metadata, List<String> warnings) {
        super(requestId, analysisType, startTime, endTime, metadata, warnings);
    }
    
    /**
     * Constructor for failed JAR upgrade analysis result.
     * 
     * @param requestId Unique identifier matching the request
     * @param analysisType Type of analysis performed
     * @param startTime When analysis began
     * @param endTime When analysis completed
     * @param errorMessage Error message describing the failure
     */
    public JarUpgradeResult(String requestId, AnalysisType analysisType,
                          LocalDateTime startTime, LocalDateTime endTime,
                          String errorMessage) {
        super(requestId, analysisType, startTime, endTime, errorMessage);
    }
    
    // Tool-specific result getters and setters
    
    public Object getDependencyAnalysis() {
        return dependencyAnalysis;
    }
    
    public void setDependencyAnalysis(Object dependencyAnalysis) {
        this.dependencyAnalysis = dependencyAnalysis;
    }
    
    public Object getApiUsageAnalysis() {
        return apiUsageAnalysis;
    }
    
    public void setApiUsageAnalysis(Object apiUsageAnalysis) {
        this.apiUsageAnalysis = apiUsageAnalysis;
    }
    
    public Object getDeprecatedApiAnalysis() {
        return deprecatedApiAnalysis;
    }
    
    public void setDeprecatedApiAnalysis(Object deprecatedApiAnalysis) {
        this.deprecatedApiAnalysis = deprecatedApiAnalysis;
    }
    
    public Object getVulnerabilityAnalysis() {
        return vulnerabilityAnalysis;
    }
    
    public void setVulnerabilityAnalysis(Object vulnerabilityAnalysis) {
        this.vulnerabilityAnalysis = vulnerabilityAnalysis;
    }
    
    
    /**
     * Gets the total duration of the analysis.
     * 
     * @return Duration in milliseconds
     */
    public long getDurationMillis() {
        return getDurationMs(); // Use the parent class method
    }
    
    /**
     * Checks if the analysis completed successfully.
     * 
     * @return true if analysis completed without critical errors
     */
    public boolean wasSuccessful() {
        return isSuccessful(); // Use the parent class method
    }
    
    @Override
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("JAR Upgrade Impact Analysis Results:\n");
        
        if (dependencyAnalysis != null) {
            summary.append("- Dependency Analysis: Completed\n");
        }
        if (apiUsageAnalysis != null) {
            summary.append("- API Usage Analysis: Completed\n");
        }
        if (deprecatedApiAnalysis != null) {
            summary.append("- Deprecated API Analysis: Completed\n");
        }
        if (vulnerabilityAnalysis != null) {
            summary.append("- Vulnerability Analysis: Completed\n");
        }
        
        summary.append(String.format("Duration: %d ms, Status: %s", 
                                    getDurationMs(), getStatus()));
        
        if (!getWarnings().isEmpty()) {
            summary.append(String.format(", Warnings: %d", getWarnings().size()));
        }
        
        return summary.toString();
    }
    
    @Override
    public int getTotalFindings() {
        int findings = 0;
        
        // Count findings from each analysis component
        // In a real implementation, these would be properly typed objects
        // with countable findings
        if (dependencyAnalysis != null) findings += 1;
        if (apiUsageAnalysis != null) findings += 1;
        if (deprecatedApiAnalysis != null) findings += 1;
        if (vulnerabilityAnalysis != null) findings += 1;
        
        return findings;
    }
    
    
    @Override
    public String toString() {
        return String.format("JarUpgradeResult{requestId='%s', duration=%dms, warnings=%d, successful=%s}",
                getRequestId(), getDurationMillis(), getWarnings().size(), wasSuccessful());
    }
}
