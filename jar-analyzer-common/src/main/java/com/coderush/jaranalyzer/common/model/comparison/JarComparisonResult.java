package com.coderush.jaranalyzer.common.model.comparison;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.model.AnalysisType;

/**
 * Result model for JAR Comparison Analysis.
 * 
 * Contains comprehensive comparison results including:
 * - Summary statistics (total changes, breaking changes, etc.)
 * - Detailed change listings organized by type
 * - Binary compatibility analysis
 * - Performance metrics for the analysis
 * 
 * Why this design:
 * - Extends AnalysisResult for consistent result handling
 * - Structured data allows frontend to build rich visualizations
 * - Summary statistics provide quick overview
 * - Detailed changes enable deep analysis
 * - Performance metrics help optimize analysis speed
 */
public class JarComparisonResult extends AnalysisResult {
    
    private final String oldJarName;
    private final String newJarName;
    private final List<ChangeDetail> changes;
    private final ComparisonSummary comparisonSummary;
    private final int oldJarClassCount;
    private final int newJarClassCount;
    
    public JarComparisonResult(String requestId, String oldJarName, String newJarName,
                              List<ChangeDetail> changes, LocalDateTime startTime, LocalDateTime endTime,
                              int oldJarClassCount, int newJarClassCount, List<String> warnings) {
        super(requestId, AnalysisType.JAR_COMPARISON, startTime, endTime, 
              createMetadata(oldJarName, newJarName, oldJarClassCount, newJarClassCount), warnings);
        
        this.oldJarName = oldJarName;
        this.newJarName = newJarName;
        this.changes = new ArrayList<>(changes);
        this.oldJarClassCount = oldJarClassCount;
        this.newJarClassCount = newJarClassCount;
        this.comparisonSummary = generateSummary();
    }
    
    /**
     * Create metadata map for the base class
     */
    private static Map<String, Object> createMetadata(String oldJarName, String newJarName, 
                                                     int oldJarClassCount, int newJarClassCount) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("oldJarName", oldJarName);
        metadata.put("newJarName", newJarName);
        metadata.put("oldJarClassCount", oldJarClassCount);
        metadata.put("newJarClassCount", newJarClassCount);
        return metadata;
    }
    
    /**
     * Generate summary statistics from the change details
     */
    private ComparisonSummary generateSummary() {
        int totalChanges = changes.size();
        int breakingChanges = (int) changes.stream()
            .filter(ChangeDetail::isBreakingChange)
            .count();
        
        int classChanges = (int) changes.stream()
            .filter(ChangeDetail::isClassLevelChange)
            .count();
        
        int methodChanges = (int) changes.stream()
            .filter(ChangeDetail::isMethodLevelChange)
            .count();
        
        int fieldChanges = (int) changes.stream()
            .filter(ChangeDetail::isFieldLevelChange)
            .count();
        
        // Group changes by type for detailed breakdown
        Map<ChangeDetail.ChangeType, Integer> changesByType = changes.stream()
            .collect(Collectors.groupingBy(
                ChangeDetail::getType,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        // Group changes by compatibility impact
        Map<ChangeDetail.CompatibilityImpact, Integer> changesByImpact = changes.stream()
            .collect(Collectors.groupingBy(
                ChangeDetail::getCompatibilityImpact,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        return new ComparisonSummary(
            totalChanges, breakingChanges, classChanges, methodChanges, fieldChanges,
            changesByType, changesByImpact
        );
    }
    
    // Getters
    public String getOldJarName() { return oldJarName; }
    public String getNewJarName() { return newJarName; }
    public List<ChangeDetail> getChanges() { return new ArrayList<>(changes); }
    public ComparisonSummary getComparisonSummary() { return comparisonSummary; }
    public int getOldJarClassCount() { return oldJarClassCount; }
    public int getNewJarClassCount() { return newJarClassCount; }
    
    /**
     * Get changes filtered by type
     */
    public List<ChangeDetail> getChangesByType(ChangeDetail.ChangeType type) {
        return changes.stream()
            .filter(change -> change.getType() == type)
            .collect(Collectors.toList());
    }
    
    /**
     * Get changes filtered by compatibility impact
     */
    public List<ChangeDetail> getChangesByImpact(ChangeDetail.CompatibilityImpact impact) {
        return changes.stream()
            .filter(change -> change.getCompatibilityImpact() == impact)
            .collect(Collectors.toList());
    }
    
    /**
     * Get changes for a specific class
     */
    public List<ChangeDetail> getChangesForClass(String className) {
        return changes.stream()
            .filter(change -> change.getClassName().equals(className))
            .collect(Collectors.toList());
    }
    
    /**
     * Check if the comparison found any breaking changes
     */
    public boolean hasBreakingChanges() {
        return comparisonSummary.breakingChanges > 0;
    }
    
    /**
     * Implementation of abstract method from AnalysisResult
     */
    @Override
    public String getSummary() {
        return String.format(
            "Compared %s with %s: %d total changes (%d breaking, %d class-level, %d method-level, %d field-level)",
            oldJarName, newJarName, comparisonSummary.totalChanges, comparisonSummary.breakingChanges,
            comparisonSummary.classChanges, comparisonSummary.methodChanges, comparisonSummary.fieldChanges
        );
    }
    
    /**
     * Implementation of abstract method from AnalysisResult
     */
    @Override
    public int getTotalFindings() {
        return changes.size();
    }
    
    /**
     * Summary statistics for the comparison
     */
    public static class ComparisonSummary {
        public final int totalChanges;
        public final int breakingChanges;
        public final int classChanges;
        public final int methodChanges;
        public final int fieldChanges;
        public final Map<ChangeDetail.ChangeType, Integer> changesByType;
        public final Map<ChangeDetail.CompatibilityImpact, Integer> changesByImpact;
        
        public ComparisonSummary(int totalChanges, int breakingChanges,
                               int classChanges, int methodChanges, int fieldChanges,
                               Map<ChangeDetail.ChangeType, Integer> changesByType,
                               Map<ChangeDetail.CompatibilityImpact, Integer> changesByImpact) {
            this.totalChanges = totalChanges;
            this.breakingChanges = breakingChanges;
            this.classChanges = classChanges;
            this.methodChanges = methodChanges;
            this.fieldChanges = fieldChanges;
            this.changesByType = new HashMap<>(changesByType);
            this.changesByImpact = new HashMap<>(changesByImpact);
        }
    }
}
