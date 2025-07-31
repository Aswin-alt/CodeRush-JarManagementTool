package com.coderush.jaranalyzer.core.service.comparison.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.coderush.jaranalyzer.common.model.comparison.ChangeDetail;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonRequest;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonResult;
import com.coderush.jaranalyzer.core.service.comparison.JarComparisonService;

/**
 * ASM-based implementation of JAR comparison service.
 * 
 * This implementation uses the ASM library to perform deep bytecode analysis
 * and identify all changes between two JAR versions.
 * 
 * Why ASM for this implementation:
 * - Provides complete access to bytecode structure
 * - Can analyze method signatures, parameters, return types
 * - Detects access modifier changes (public -> private, etc.)
 * - Handles annotation changes and metadata
 * - Performance optimized for large JAR files
 * - Works with obfuscated code (analyzes actual bytecode)
 * 
 * Analysis Process:
 * 1. Load and scan both JAR files to extract class metadata
 * 2. Compare class-level changes (added, removed, modified)
 * 3. For each common class, compare methods and fields
 * 4. Generate detailed change reports with compatibility impact
 * 5. Classify changes by type and severity
 */
public class AsmJarComparisonService implements JarComparisonService {
    
    private static final Logger logger = LoggerFactory.getLogger(AsmJarComparisonService.class);
    
    
    /**
     * Interface implementation - Compare two JAR files using the request model.
     * 
     * This method bridges the interface contract with our internal implementation.
     */
    @Override
    public JarComparisonResult compareJars(JarComparisonRequest request) throws AnalysisException {
        logger.info("Starting JAR comparison with request: {}", request.getRequestId());
        
        // Validate request first
        validateComparisonRequest(request);
        
        // Delegate to internal implementation
        return compareJarsInternal(
            request.getRequestId(),
            request.getOldJarFile(),
            request.getNewJarFile(),
            request.isIncludePrivateMembers(),
            request.isIncludePackageClasses(),
            request.isAnalyzeFieldChanges(),
            request.isAnalyzeAnnotations()
        );
    }
    
    /**
     * Interface implementation - Validate comparison request.
     * 
     * Performs comprehensive validation of the request before analysis.
     */
    @Override
    public void validateComparisonRequest(JarComparisonRequest request) throws AnalysisException {
        logger.debug("Validating JAR comparison request: {}", request.getRequestId());
        
        try {
            // Use the request's built-in validation
            request.validate();
            
            // Additional ASM-specific validations
            validateJarForAsmCompatibility(request.getOldJarFile(), "old JAR");
            validateJarForAsmCompatibility(request.getNewJarFile(), "new JAR");
            
            logger.debug("JAR comparison request validation successful");
            
        } catch (IllegalArgumentException e) {
            throw new AnalysisException(
                com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                "Request validation failed: " + e.getMessage(), 
                e
            );
        } catch (Exception e) {
            throw new AnalysisException(
                com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                "Unexpected validation error: " + e.getMessage(), 
                e
            );
        }
    }
    
    /**
     * Validate that a JAR file can be processed by ASM.
     * 
     * This checks for ASM-specific requirements like readable class files.
     */
    private void validateJarForAsmCompatibility(File jarFile, String jarDescription) throws AnalysisException {
        try (JarFile jar = new JarFile(jarFile)) {
            
            // Check if JAR contains at least one .class file
            boolean hasClassFiles = jar.stream()
                .anyMatch(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"));
            
            if (!hasClassFiles) {
                throw new AnalysisException(
                    com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                    jarDescription + " contains no .class files: " + jarFile.getName()
                );
            }
            
            // Try to read at least one class file to verify ASM compatibility
            jar.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                .findFirst()
                .ifPresent(entry -> {
                    try (InputStream inputStream = jar.getInputStream(entry)) {
                        new ClassReader(inputStream); // This will throw if incompatible
                        logger.debug("ASM compatibility check passed for {}", jarDescription);
                    } catch (Exception e) {
                        throw new RuntimeException("ASM compatibility check failed for " + jarDescription + 
                            ": " + e.getMessage(), e);
                    }
                });
                
        } catch (IOException e) {
            throw new AnalysisException(
                com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                "Failed to open " + jarDescription + " for validation: " + e.getMessage(), 
                e
            );
        } catch (RuntimeException e) {
            if (e.getCause() instanceof Exception) {
                throw new AnalysisException(
                    com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                    e.getMessage(), 
                    (Exception) e.getCause()
                );
            }
            throw e;
        }
    }

    /**
     * Internal implementation - Compare two JAR files and generate detailed change report.
     * 
     * @param requestId Unique identifier for this analysis request
     * @param oldJarFile The baseline JAR file (lower version)
     * @param newJarFile The target JAR file (higher version)  
     * @param includePrivateMembers Whether to include private methods/fields
     * @param includePackageClasses Whether to include package-private classes
     * @param analyzeFieldChanges Whether to analyze field-level changes
     * @param analyzeAnnotations Whether to analyze annotation changes
     * @return Detailed comparison result
     * @throws AnalysisException if comparison fails
     */
    private JarComparisonResult compareJarsInternal(String requestId, File oldJarFile, File newJarFile,
                                          boolean includePrivateMembers, boolean includePackageClasses,
                                          boolean analyzeFieldChanges, boolean analyzeAnnotations) 
            throws AnalysisException {
        
        logger.info("Starting JAR comparison: {} vs {}", oldJarFile.getName(), newJarFile.getName());
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            // Extract class metadata from both JARs
            Map<String, ClassInfo> oldClasses = extractClassInfo(oldJarFile, includePrivateMembers, 
                includePackageClasses, analyzeFieldChanges, analyzeAnnotations);
            Map<String, ClassInfo> newClasses = extractClassInfo(newJarFile, includePrivateMembers, 
                includePackageClasses, analyzeFieldChanges, analyzeAnnotations);
            
            logger.info("Extracted {} classes from old JAR, {} classes from new JAR", 
                oldClasses.size(), newClasses.size());
            
            // Compare and generate change details
            List<ChangeDetail> changes = compareClasses(oldClasses, newClasses, 
                analyzeFieldChanges, analyzeAnnotations);
            
            LocalDateTime endTime = LocalDateTime.now();
            List<String> warnings = new ArrayList<>();
            
            logger.info("JAR comparison completed. Found {} changes", changes.size());
            
            return new JarComparisonResult(
                requestId,
                oldJarFile.getName(),
                newJarFile.getName(),
                changes,
                startTime,
                endTime,
                oldClasses.size(),
                newClasses.size(),
                warnings
            );
            
        } catch (IOException e) {
            throw new AnalysisException(com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                "Failed to read JAR files: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new AnalysisException(com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON,
                "JAR comparison failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Extract class information from a JAR file using ASM.
     * 
     * This method scans all .class files in the JAR and uses ASM ClassReader
     * to extract detailed metadata about each class.
     */
    private Map<String, ClassInfo> extractClassInfo(File jarFile, boolean includePrivateMembers,
                                                   boolean includePackageClasses, boolean analyzeFieldChanges,
                                                   boolean analyzeAnnotations) throws IOException {
        
        Map<String, ClassInfo> classInfoMap = new HashMap<>();
        
        try (JarFile jar = new JarFile(jarFile)) {
            jar.stream()
                .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(".class"))
                .forEach(entry -> {
                    try {
                        ClassInfo classInfo = extractSingleClassInfo(jar, entry, includePrivateMembers,
                            includePackageClasses, analyzeFieldChanges, analyzeAnnotations);
                        if (classInfo != null) {
                            classInfoMap.put(classInfo.className, classInfo);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to analyze class {}: {}", entry.getName(), e.getMessage());
                    }
                });
        }
        
        return classInfoMap;
    }
    
    /**
     * Extract information from a single class file using ASM ClassReader.
     */
    private ClassInfo extractSingleClassInfo(JarFile jar, JarEntry entry, boolean includePrivateMembers,
                                            boolean includePackageClasses, boolean analyzeFieldChanges,
                                            boolean analyzeAnnotations) throws IOException {
        
        try (InputStream inputStream = jar.getInputStream(entry)) {
            ClassReader classReader = new ClassReader(inputStream);
            ClassInfoExtractor extractor = new ClassInfoExtractor(includePrivateMembers,
                includePackageClasses, analyzeFieldChanges, analyzeAnnotations);
            
            classReader.accept(extractor, ClassReader.SKIP_CODE); // Skip method bodies for performance
            return extractor.getClassInfo();
        }
    }
    
    /**
     * Compare two sets of class information and generate change details.
     */
    private List<ChangeDetail> compareClasses(Map<String, ClassInfo> oldClasses, 
                                            Map<String, ClassInfo> newClasses,
                                            boolean analyzeFieldChanges, boolean analyzeAnnotations) {
        
        List<ChangeDetail> changes = new ArrayList<>();
        
        // Find removed classes
        for (String className : oldClasses.keySet()) {
            if (!newClasses.containsKey(className)) {
                changes.add(new ChangeDetail(
                    ChangeDetail.ChangeType.CLASS_REMOVED,
                    className,
                    null, // No member name for class-level changes
                    oldClasses.get(className).getClassSignature(),
                    null,
                    "Class " + className + " was removed",
                    ChangeDetail.CompatibilityImpact.BREAKING,
                    List.of("Class no longer exists in the new version")
                ));
            }
        }
        
        // Find added classes
        for (String className : newClasses.keySet()) {
            if (!oldClasses.containsKey(className)) {
                changes.add(new ChangeDetail(
                    ChangeDetail.ChangeType.CLASS_ADDED,
                    className,
                    null,
                    null,
                    newClasses.get(className).getClassSignature(),
                    "Class " + className + " was added",
                    ChangeDetail.CompatibilityImpact.NONE,
                    List.of("New class added to the library")
                ));
            }
        }
        
        // Find modified classes
        for (String className : oldClasses.keySet()) {
            if (newClasses.containsKey(className)) {
                ClassInfo oldClass = oldClasses.get(className);
                ClassInfo newClass = newClasses.get(className);
                changes.addAll(compareClassDetails(oldClass, newClass, analyzeFieldChanges, analyzeAnnotations));
            }
        }
        
        return changes;
    }
    
    /**
     * Compare details of two versions of the same class.
     */
    private List<ChangeDetail> compareClassDetails(ClassInfo oldClass, ClassInfo newClass,
                                                 boolean analyzeFieldChanges, boolean analyzeAnnotations) {
        
        List<ChangeDetail> changes = new ArrayList<>();
        
        // Compare methods
        changes.addAll(compareMethods(oldClass, newClass));
        
        // Compare fields if enabled
        if (analyzeFieldChanges) {
            changes.addAll(compareFields(oldClass, newClass));
        }
        
        // Compare annotations if enabled
        if (analyzeAnnotations) {
            changes.addAll(compareAnnotations(oldClass, newClass));
        }
        
        return changes;
    }
    
    /**
     * Compare methods between two class versions.
     */
    private List<ChangeDetail> compareMethods(ClassInfo oldClass, ClassInfo newClass) {
        List<ChangeDetail> changes = new ArrayList<>();
        
        String className = oldClass.className;
        
        // Find removed methods
        for (MethodInfo oldMethod : oldClass.methods) {
            MethodInfo newMethod = findMethod(newClass.methods, oldMethod.name, oldMethod.descriptor);
            if (newMethod == null) {
                changes.add(new ChangeDetail(
                    ChangeDetail.ChangeType.METHOD_REMOVED,
                    className,
                    oldMethod.name,
                    oldMethod.getSignature(),
                    null,
                    "Method " + oldMethod.name + " was removed from class " + className,
                    ChangeDetail.CompatibilityImpact.BREAKING,
                    List.of("Method no longer exists", "Calling code will fail at runtime")
                ));
            }
        }
        
        // Find added methods
        for (MethodInfo newMethod : newClass.methods) {
            MethodInfo oldMethod = findMethod(oldClass.methods, newMethod.name, newMethod.descriptor);
            if (oldMethod == null) {
                changes.add(new ChangeDetail(
                    ChangeDetail.ChangeType.METHOD_ADDED,
                    className,
                    newMethod.name,
                    null,
                    newMethod.getSignature(),
                    "Method " + newMethod.name + " was added to class " + className,
                    ChangeDetail.CompatibilityImpact.NONE,
                    List.of("New method available for use")
                ));
            }
        }
        
        // Find modified methods
        for (MethodInfo oldMethod : oldClass.methods) {
            MethodInfo newMethod = findMethod(newClass.methods, oldMethod.name, oldMethod.descriptor);
            if (newMethod != null) {
                changes.addAll(compareMethodDetails(className, oldMethod, newMethod));
            }
        }
        
        return changes;
    }
    
    /**
     * Compare fields between two class versions.
     */
    private List<ChangeDetail> compareFields(ClassInfo oldClass, ClassInfo newClass) {
        List<ChangeDetail> changes = new ArrayList<>();
        
        String className = oldClass.className;
        
        // Find removed fields
        for (FieldInfo oldField : oldClass.fields) {
            FieldInfo newField = findField(newClass.fields, oldField.name);
            if (newField == null) {
                changes.add(new ChangeDetail(
                    ChangeDetail.ChangeType.FIELD_REMOVED,
                    className,
                    oldField.name,
                    oldField.getSignature(),
                    null,
                    "Field " + oldField.name + " was removed from class " + className,
                    ChangeDetail.CompatibilityImpact.BREAKING,
                    List.of("Field no longer exists", "Code accessing this field will fail")
                ));
            }
        }
        
        // Find added fields
        for (FieldInfo newField : newClass.fields) {
            FieldInfo oldField = findField(oldClass.fields, newField.name);
            if (oldField == null) {
                changes.add(new ChangeDetail(
                    ChangeDetail.ChangeType.FIELD_ADDED,
                    className,
                    newField.name,
                    null,
                    newField.getSignature(),
                    "Field " + newField.name + " was added to class " + className,
                    ChangeDetail.CompatibilityImpact.NONE,
                    List.of("New field available for use")
                ));
            }
        }
        
        // Find modified fields
        for (FieldInfo oldField : oldClass.fields) {
            FieldInfo newField = findField(newClass.fields, oldField.name);
            if (newField != null) {
                changes.addAll(compareFieldDetails(className, oldField, newField));
            }
        }
        
        return changes;
    }
    
    /**
     * Compare annotations between two class versions.
     */
    private List<ChangeDetail> compareAnnotations(ClassInfo oldClass, ClassInfo newClass) {
        List<ChangeDetail> changes = new ArrayList<>();
        // TODO: Implement annotation comparison logic
        // This would involve comparing annotation lists and their values
        return changes;
    }
    
    /**
     * Compare details of two versions of the same method.
     */
    private List<ChangeDetail> compareMethodDetails(String className, MethodInfo oldMethod, MethodInfo newMethod) {
        List<ChangeDetail> changes = new ArrayList<>();
        
        // Compare access modifiers
        if (oldMethod.access != newMethod.access) {
            String oldAccess = getAccessModifierString(oldMethod.access);
            String newAccess = getAccessModifierString(newMethod.access);
            
            ChangeDetail.CompatibilityImpact impact = determineAccessModifierImpact(oldMethod.access, newMethod.access);
            
            changes.add(new ChangeDetail(
                ChangeDetail.ChangeType.METHOD_ACCESS_CHANGED,
                className,
                oldMethod.name,
                oldAccess,
                newAccess,
                "Method " + oldMethod.name + " access changed from " + oldAccess + " to " + newAccess,
                impact,
                List.of("Access modifier change may affect calling code")
            ));
        }
        
        return changes;
    }
    
    /**
     * Compare details of two versions of the same field.
     */
    private List<ChangeDetail> compareFieldDetails(String className, FieldInfo oldField, FieldInfo newField) {
        List<ChangeDetail> changes = new ArrayList<>();
        
        // Compare field types
        if (!oldField.descriptor.equals(newField.descriptor)) {
            changes.add(new ChangeDetail(
                ChangeDetail.ChangeType.FIELD_TYPE_CHANGED,
                className,
                oldField.name,
                oldField.descriptor,
                newField.descriptor,
                "Field " + oldField.name + " type changed",
                ChangeDetail.CompatibilityImpact.BREAKING,
                List.of("Field type change breaks binary compatibility")
            ));
        }
        
        // Compare access modifiers
        if (oldField.access != newField.access) {
            String oldAccess = getAccessModifierString(oldField.access);
            String newAccess = getAccessModifierString(newField.access);
            
            ChangeDetail.CompatibilityImpact impact = determineAccessModifierImpact(oldField.access, newField.access);
            
            changes.add(new ChangeDetail(
                ChangeDetail.ChangeType.FIELD_ACCESS_CHANGED,
                className,
                oldField.name,
                oldAccess,
                newAccess,
                "Field " + oldField.name + " access changed from " + oldAccess + " to " + newAccess,
                impact,
                List.of("Access modifier change may affect field access")
            ));
        }
        
        return changes;
    }
    
    // Helper methods
    
    private MethodInfo findMethod(List<MethodInfo> methods, String name, String descriptor) {
        return methods.stream()
            .filter(m -> m.name.equals(name) && m.descriptor.equals(descriptor))
            .findFirst()
            .orElse(null);
    }
    
    private FieldInfo findField(List<FieldInfo> fields, String name) {
        return fields.stream()
            .filter(f -> f.name.equals(name))
            .findFirst()
            .orElse(null);
    }
    
    private String getAccessModifierString(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) return "public";
        if ((access & Opcodes.ACC_PROTECTED) != 0) return "protected";
        if ((access & Opcodes.ACC_PRIVATE) != 0) return "private";
        return "package-private";
    }
    
    private ChangeDetail.CompatibilityImpact determineAccessModifierImpact(int oldAccess, int newAccess) {
        // Simplified logic: reducing visibility is breaking, increasing is safe
        int oldVisibility = getVisibilityLevel(oldAccess);
        int newVisibility = getVisibilityLevel(newAccess);
        
        if (newVisibility < oldVisibility) {
            return ChangeDetail.CompatibilityImpact.BREAKING; // Reduced visibility
        } else if (newVisibility > oldVisibility) {
            return ChangeDetail.CompatibilityImpact.NONE; // Increased visibility
        } else {
            return ChangeDetail.CompatibilityImpact.LOW; // Same visibility but different modifier
        }
    }
    
    private int getVisibilityLevel(int access) {
        if ((access & Opcodes.ACC_PUBLIC) != 0) return 3;
        if ((access & Opcodes.ACC_PROTECTED) != 0) return 2;
        if ((access & Opcodes.ACC_PRIVATE) != 0) return 0;
        return 1; // package-private
    }
    
    // Data classes for storing class information
    
    private static class ClassInfo {
        String className;
        int access;
        String superName;
        String[] interfaces;
        List<MethodInfo> methods = new ArrayList<>();
        List<FieldInfo> fields = new ArrayList<>();
        List<String> annotations = new ArrayList<>();
        
        String getClassSignature() {
            return className + " extends " + superName;
        }
    }
    
    private static class MethodInfo {
        String name;
        String descriptor;
        int access;
        String[] exceptions;
        List<String> annotations = new ArrayList<>();
        
        String getSignature() {
            return getAccessModifierString(access) + " " + name + descriptor;
        }
        
        private String getAccessModifierString(int access) {
            if ((access & Opcodes.ACC_PUBLIC) != 0) return "public";
            if ((access & Opcodes.ACC_PROTECTED) != 0) return "protected";
            if ((access & Opcodes.ACC_PRIVATE) != 0) return "private";
            return "package-private";
        }
    }
    
    private static class FieldInfo {
        String name;
        String descriptor;
        int access;
        Object value;
        List<String> annotations = new ArrayList<>();
        
        String getSignature() {
            return getAccessModifierString(access) + " " + descriptor + " " + name;
        }
        
        private String getAccessModifierString(int access) {
            if ((access & Opcodes.ACC_PUBLIC) != 0) return "public";
            if ((access & Opcodes.ACC_PROTECTED) != 0) return "protected";
            if ((access & Opcodes.ACC_PRIVATE) != 0) return "private";
            return "package-private";
        }
    }
    
    /**
     * ASM ClassVisitor to extract class information.
     */
    private static class ClassInfoExtractor extends ClassVisitor {
        private final boolean includePrivateMembers;
        private final boolean includePackageClasses;
        private final boolean analyzeFieldChanges;
        private final boolean analyzeAnnotations;
        
        private ClassInfo classInfo;
        
        public ClassInfoExtractor(boolean includePrivateMembers, boolean includePackageClasses,
                                boolean analyzeFieldChanges, boolean analyzeAnnotations) {
            super(Opcodes.ASM9);
            this.includePrivateMembers = includePrivateMembers;
            this.includePackageClasses = includePackageClasses;
            this.analyzeFieldChanges = analyzeFieldChanges;
            this.analyzeAnnotations = analyzeAnnotations;
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            // Filter based on class visibility
            if (!includePackageClasses && (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0) {
                return; // Skip package-private classes
            }
            
            classInfo = new ClassInfo();
            classInfo.className = name.replace('/', '.');
            classInfo.access = access;
            classInfo.superName = superName != null ? superName.replace('/', '.') : null;
            classInfo.interfaces = interfaces;
        }
        
        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (classInfo == null) return null;
            
            // Filter based on method visibility
            if (!includePrivateMembers && (access & Opcodes.ACC_PRIVATE) != 0) {
                return null; // Skip private methods
            }
            
            MethodInfo methodInfo = new MethodInfo();
            methodInfo.name = name;
            methodInfo.descriptor = descriptor;
            methodInfo.access = access;
            methodInfo.exceptions = exceptions;
            
            classInfo.methods.add(methodInfo);
            return null; // We don't need to visit method bodies
        }
        
        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (classInfo == null || !analyzeFieldChanges) return null;
            
            // Filter based on field visibility
            if (!includePrivateMembers && (access & Opcodes.ACC_PRIVATE) != 0) {
                return null; // Skip private fields
            }
            
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.name = name;
            fieldInfo.descriptor = descriptor;
            fieldInfo.access = access;
            fieldInfo.value = value;
            
            classInfo.fields.add(fieldInfo);
            return null; // We don't need field visitors for now
        }
        
        public ClassInfo getClassInfo() {
            return classInfo;
        }
    }
}
