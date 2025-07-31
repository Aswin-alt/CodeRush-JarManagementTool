package com.coderush.jaranalyzer.core.demo;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coderush.jaranalyzer.common.model.comparison.JarComparisonResult;
import com.coderush.jaranalyzer.core.analyzer.JarComparisonAnalyzer;
import com.coderush.jaranalyzer.core.service.comparison.impl.AsmJarComparisonService;

/**
 * Demonstration of the JAR Comparison feature (Feature 1).
 * 
 * This class shows how to use the complete modular architecture:
 * 1. Create a comparison request with two JAR files
 * 2. Configure analysis options (private members, fields, annotations)
 * 3. Use the analyzer to orchestrate the comparison
 * 4. Process and display the results
 * 
 * Architecture Flow Demonstrated:
 * Request → Analyzer → Service → ASM Implementation → Results
 * 
 * This demonstrates the clean separation between:
 * - Request/Result models (common layer)
 * - Analysis orchestration (analyzer layer)
 * - Comparison logic (service layer)
 * - ASM implementation (implementation layer)
 */
public class JarComparisonDemo {
    
    private static final Logger logger = LoggerFactory.getLogger(JarComparisonDemo.class);
    
    public static void main(String[] args) {
        logger.info("Starting JAR Comparison Demo");
        
        try {
            // For demo purposes, we'll show the complete workflow structure
            // In a real application, these would be actual JAR files provided by the user
            
            demonstrateArchitecture();
            demonstrateUsageScenarios();
            
        } catch (Exception e) {
            logger.error("Demo failed: {}", e.getMessage(), e);
        }
        
        logger.info("JAR Comparison Demo completed");
    }
    
    /**
     * Demonstrates the complete modular architecture.
     */
    private static void demonstrateArchitecture() {
        logger.info("=== JAR Comparison Architecture Demonstration ===");
        
        // Step 1: Initialize the service layer
        AsmJarComparisonService comparisonService = new AsmJarComparisonService();
        logger.info("✓ ASM-based comparison service initialized");
        
        // Step 2: Initialize the analyzer layer
        JarComparisonAnalyzer analyzer = new JarComparisonAnalyzer(comparisonService);
        logger.info("✓ JAR comparison analyzer initialized");
        logger.info("✓ Service metadata: {}", comparisonService.getServiceMetadata().getName());
        logger.info("✓ Analyzer metadata: {}", analyzer.getMetadata().getName());
        
        // Step 3: Show configuration capabilities
        logger.info("✓ Analysis capabilities:");
        logger.info("  - Supports: {}", analyzer.getAnalysisType());
        logger.info("  - Version: {}", analyzer.getMetadata().getVersion());
        logger.info("  - Required tools: {}", String.join(", ", analyzer.getMetadata().getRequiredTools()));
        
        logger.info("Architecture demonstration completed successfully");
    }
    
    /**
     * Demonstrates different usage scenarios.
     */
    private static void demonstrateUsageScenarios() {
        logger.info("=== JAR Comparison Usage Scenarios ===");
        
        // Scenario 1: Basic comparison with default options
        logger.info("Scenario 1: Basic JAR comparison");
        demonstrateBasicComparison();
        
        // Scenario 2: Advanced comparison with all options enabled
        logger.info("Scenario 2: Advanced JAR comparison with all features");
        demonstrateAdvancedComparison();
        
        // Scenario 3: Validation and error handling
        logger.info("Scenario 3: Request validation and error handling");
        demonstrateValidationAndErrorHandling();
    }
    
    /**
     * Shows basic JAR comparison with default options.
     */
    private static void demonstrateBasicComparison() {
        try {
            // Note: In a real application, these would be actual JAR files
            // For demo purposes, we're showing the API structure
            
            logger.info("Creating basic comparison request...");
            // File oldJar = new File("path/to/old-version.jar");
            // File newJar = new File("path/to/new-version.jar");
            // JarComparisonRequest request = new JarComparisonRequest(oldJar, newJar);
            
            logger.info("Basic comparison request structure:");
            logger.info("  - Old JAR: [path/to/old-version.jar]");
            logger.info("  - New JAR: [path/to/new-version.jar]");
            logger.info("  - Include private members: false (default)");
            logger.info("  - Analyze field changes: true (default)");
            logger.info("  - Analyze annotations: true (default)");
            
            // In real usage:
            // JarComparisonAnalyzer analyzer = new JarComparisonAnalyzer(new AsmJarComparisonService());
            // JarComparisonResult result = analyzer.analyze(request);
            // processResults(result);
            
            logger.info("✓ Basic comparison scenario demonstrated");
            
        } catch (Exception e) {
            logger.error("Basic comparison demonstration failed: {}", e.getMessage());
        }
    }
    
    /**
     * Shows advanced JAR comparison with all features enabled.
     */
    private static void demonstrateAdvancedComparison() {
        try {
            logger.info("Creating advanced comparison request...");
            
            // Advanced configuration with all options enabled
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("includePrivateMembers", true);
            metadata.put("includePackageClasses", true);
            metadata.put("analyzeFieldChanges", true);
            metadata.put("analyzeAnnotations", true);
            metadata.put("detectBinaryCompatibility", true);
            
            logger.info("Advanced comparison configuration:");
            logger.info("  - Include private members: true");
            logger.info("  - Include package classes: true");
            logger.info("  - Analyze field changes: true");
            logger.info("  - Analyze annotations: true");
            logger.info("  - Detect binary compatibility: true");
            
            // In real usage:
            // JarComparisonRequest request = new JarComparisonRequest(oldJar, newJar, metadata);
            // long estimatedDuration = analyzer.estimateDuration(request);
            // logger.info("Estimated analysis time: {}ms", estimatedDuration);
            
            logger.info("✓ Advanced comparison scenario demonstrated");
            
        } catch (Exception e) {
            logger.error("Advanced comparison demonstration failed: {}", e.getMessage());
        }
    }
    
    /**
     * Demonstrates request validation and error handling.
     */
    private static void demonstrateValidationAndErrorHandling() {
        try {
            logger.info("Demonstrating validation capabilities...");
            
            AsmJarComparisonService service = new AsmJarComparisonService();
            JarComparisonAnalyzer analyzer = new JarComparisonAnalyzer(service);
            
            // Test analyzer capabilities
            logger.info("Analyzer supports JAR_COMPARISON: {}", 
                analyzer.supports(com.coderush.jaranalyzer.common.model.AnalysisType.JAR_COMPARISON));
            logger.info("Analyzer supports JAR_UPGRADE_IMPACT: {}", 
                analyzer.supports(com.coderush.jaranalyzer.common.model.AnalysisType.JAR_UPGRADE_IMPACT));
            
            // Validation would check:
            logger.info("Validation checks include:");
            logger.info("  ✓ File existence and readability");
            logger.info("  ✓ Valid JAR format");
            logger.info("  ✓ ASM compatibility");
            logger.info("  ✓ Non-empty class files");
            logger.info("  ✓ Request parameter validation");
            
            logger.info("✓ Validation and error handling demonstrated");
            
        } catch (Exception e) {
            logger.error("Validation demonstration failed: {}", e.getMessage());
        }
    }
    
    /**
     * Shows how to process comparison results.
     * 
     * This would be called in real usage after successful analysis.
     */
    @SuppressWarnings("unused")
    private static void processResults(JarComparisonResult result) {
        logger.info("=== Processing JAR Comparison Results ===");
        
        logger.info("Comparison completed: {} vs {}", 
            result.getOldJarName(), result.getNewJarName());
        logger.info("Analysis duration: {}ms", 
            java.time.Duration.between(result.getStartTime(), result.getEndTime()).toMillis());
        
        logger.info("Summary:");
        logger.info("  - Total changes: {}", result.getChanges().size());
        logger.info("  - Old JAR classes: {}", result.getOldJarClassCount());
        logger.info("  - New JAR classes: {}", result.getNewJarClassCount());
        
        // Process each change
        result.getChanges().forEach(change -> {
            logger.info("Change: {} - {} ({})", 
                change.getType(), 
                change.getDescription(),
                change.getCompatibilityImpact());
        });
        
        // Show warnings
        if (!result.getWarnings().isEmpty()) {
            logger.info("Warnings:");
            result.getWarnings().forEach(warning -> logger.info("  ⚠ {}", warning));
        }
    }
}
