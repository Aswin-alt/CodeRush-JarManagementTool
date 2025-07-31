package com.coderush.jaranalyzer.common.model.comparison;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a detailed change found during JAR comparison.
 * 
 * This model captures specific changes at class, method, or field level
 * with enough detail to understand what changed and why it matters.
 * 
 * Why this design:
 * - Immutable value object for thread safety
 * - Hierarchical change types for organized reporting
 * - Rich metadata allows detailed impact analysis
 * - Binary compatibility flags help developers understand upgrade risks
 */
public class ChangeDetail {
    
    /**
     * Types of changes that can be detected
     */
    public enum ChangeType {
        // Class-level changes
        CLASS_ADDED,           // New class added
        CLASS_REMOVED,         // Existing class removed
        CLASS_MODIFIED,        // Class signature changed
        
        // Method-level changes
        METHOD_ADDED,          // New method added
        METHOD_REMOVED,        // Existing method removed
        METHOD_SIGNATURE_CHANGED,  // Method signature modified
        METHOD_RETURN_TYPE_CHANGED, // Return type changed
        METHOD_PARAMETER_CHANGED,   // Parameters changed
        METHOD_ACCESS_CHANGED,      // Access modifier changed
        
        // Field-level changes
        FIELD_ADDED,           // New field added
        FIELD_REMOVED,         // Existing field removed
        FIELD_TYPE_CHANGED,    // Field type changed
        FIELD_ACCESS_CHANGED,  // Field access modifier changed
        
        // Annotation changes
        ANNOTATION_ADDED,      // New annotation added
        ANNOTATION_REMOVED,    // Existing annotation removed
        ANNOTATION_MODIFIED    // Annotation values changed
    }
    
    /**
     * Impact level of the change on binary compatibility
     */
    public enum CompatibilityImpact {
        NONE,           // No impact on compatibility
        LOW,            // Minor impact, usually safe
        MEDIUM,         // Moderate impact, may break some usages
        HIGH,           // High impact, likely to break existing code
        BREAKING        // Definitely breaking change
    }
    
    private final ChangeType type;
    private final String className;
    private final String memberName;        // Method or field name (null for class-level changes)
    private final String oldSignature;     // Previous signature/type
    private final String newSignature;     // New signature/type
    private final String description;      // Human-readable change description
    private final CompatibilityImpact compatibilityImpact;
    private final List<String> reasons;    // Detailed reasons for the change
    
    public ChangeDetail(ChangeType type, String className, String memberName,
                       String oldSignature, String newSignature, String description,
                       CompatibilityImpact compatibilityImpact, List<String> reasons) {
        this.type = Objects.requireNonNull(type, "Change type cannot be null");
        this.className = Objects.requireNonNull(className, "Class name cannot be null");
        this.memberName = memberName;
        this.oldSignature = oldSignature;
        this.newSignature = newSignature;
        this.description = Objects.requireNonNull(description, "Description cannot be null");
        this.compatibilityImpact = Objects.requireNonNull(compatibilityImpact, "Compatibility impact cannot be null");
        this.reasons = reasons != null ? new ArrayList<>(reasons) : new ArrayList<>();
    }
    
    // Getters
    public ChangeType getType() { return type; }
    public String getClassName() { return className; }
    public String getMemberName() { return memberName; }
    public String getOldSignature() { return oldSignature; }
    public String getNewSignature() { return newSignature; }
    public String getDescription() { return description; }
    public CompatibilityImpact getCompatibilityImpact() { return compatibilityImpact; }
    public List<String> getReasons() { return new ArrayList<>(reasons); }
    
    /**
     * Checks if this is a class-level change
     */
    public boolean isClassLevelChange() {
        return memberName == null || memberName.isEmpty();
    }
    
    /**
     * Checks if this is a method-level change
     */
    public boolean isMethodLevelChange() {
        return type.name().startsWith("METHOD_");
    }
    
    /**
     * Checks if this is a field-level change
     */
    public boolean isFieldLevelChange() {
        return type.name().startsWith("FIELD_");
    }
    
    /**
     * Checks if this change is potentially breaking
     */
    public boolean isBreakingChange() {
        return compatibilityImpact == CompatibilityImpact.HIGH || 
               compatibilityImpact == CompatibilityImpact.BREAKING;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChangeDetail that = (ChangeDetail) o;
        return type == that.type &&
               Objects.equals(className, that.className) &&
               Objects.equals(memberName, that.memberName) &&
               Objects.equals(oldSignature, that.oldSignature) &&
               Objects.equals(newSignature, that.newSignature);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, className, memberName, oldSignature, newSignature);
    }
    
    @Override
    public String toString() {
        return String.format("ChangeDetail{type=%s, class='%s', member='%s', impact=%s}", 
            type, className, memberName, compatibilityImpact);
    }
}
