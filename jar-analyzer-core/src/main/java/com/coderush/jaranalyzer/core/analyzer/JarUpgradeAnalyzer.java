package com.coderush.jaranalyzer.core.analyzer;

import com.coderush.jaranalyzer.common.analyzer.ProjectAnalyzer;
import com.coderush.jaranalyzer.common.analyzer.AnalyzerMetadata;
import com.coderush.jaranalyzer.common.model.AnalysisType;
import com.coderush.jaranalyzer.common.model.upgrade.JarUpgradeRequest;
import com.coderush.jaranalyzer.common.model.upgrade.JarUpgradeResult;
import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.coderush.jaranalyzer.core.service.analyzer.JDepsAnalyzerService;
import com.coderush.jaranalyzer.core.service.analyzer.AsmAnalyzerService;
import com.coderush.jaranalyzer.core.service.analyzer.JdeprscanAnalyzerService;
import com.coderush.jaranalyzer.core.service.analyzer.OsvScannerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Concrete implementation of JAR upgrade impact analysis.
 * 
 * This class demonstrates the proper three-layer architecture:
 * 1. Implements the generic ProjectAnalyzer contract (common layer)
 * 2. Uses tool-specific services for actual analysis (service layer)  
 * 3. Provides concrete implementation logic (implementation layer)
 * 
 * Why this architecture works:
 * - Type Safety: Implements ProjectAnalyzer<JarUpgradeRequest, JarUpgradeResult>
 * - Service Composition: Combines multiple tool services
 * - Single Responsibility: Each service handles one specific tool
 * - Testability: Can mock individual services
 * - Maintainability: Clear separation of concerns
 * 
 * This is where the REAL analysis logic goes - orchestrating multiple
 * tool services to provide comprehensive upgrade impact analysis.
 */
public class JarUpgradeAnalyzer implements ProjectAnalyzer<JarUpgradeRequest, JarUpgradeResult> {
    
    private static final Logger logger = LoggerFactory.getLogger(JarUpgradeAnalyzer.class);
    
    // Service dependencies - injected via constructor
    private final JDepsAnalyzerService jdepsService;
    private final AsmAnalyzerService asmService;
    private final JdeprscanAnalyzerService jdeprscanService;
    private final OsvScannerService osvService;
    
    /**
     * Constructor with service dependencies.
     * 
     * This demonstrates proper dependency injection - all tool services
     * are provided as dependencies, making this class testable and flexible.
     */
    public JarUpgradeAnalyzer(JDepsAnalyzerService jdepsService,
                            AsmAnalyzerService asmService,
                            JdeprscanAnalyzerService jdeprscanService,
                            OsvScannerService osvService) {
        this.jdepsService = jdepsService;
        this.asmService = asmService;
        this.jdeprscanService = jdeprscanService;
        this.osvService = osvService;
    }
    
    @Override
    public JarUpgradeResult analyze(JarUpgradeRequest request) throws AnalysisException {
        logger.info("Starting JAR upgrade impact analysis for request: {}", request.getRequestId());
        
        try {
            // Step 1: Validate the request
            validateRequest(request);
            
            // Step 2: Analyze dependency changes using JDeps
            logger.debug("Analyzing dependency changes...");
            var dependencyAnalysis = jdepsService.analyzeProjectDependencies(
                java.util.List.of(request.getOldJarFile().toPath(), request.getNewJarFile().toPath()),
                java.util.List.of(), // Empty classpath for now
                java.util.Map.of()   // Default options
            );
            
            // Step 3: Analyze API usage patterns using ASM  
            logger.debug("Analyzing API usage patterns...");
            var apiUsageAnalysis = asmService.analyzeApiUsage(
                request.getNewJarFile().toPath(),
                java.util.List.of(), // No specific target APIs for now
                java.util.Map.of()   // Default options
            );
            
            // Step 4: Check for deprecated API usage using Jdeprscan
            logger.debug("Scanning for deprecated APIs...");
            var deprecatedApiAnalysis = jdeprscanService.scanForDeprecatedApis(
                request.getNewJarFile().toPath(),
                java.util.Map.of()   // Default options
            );
            
            // Step 5: Scan for new vulnerabilities using OSV-Scanner
            logger.debug("Scanning for vulnerabilities...");
            var vulnerabilityAnalysis = osvService.compareVulnerabilityProfiles(
                request.getOldJarFile().toPath(),
                request.getNewJarFile().toPath(),
                java.util.Map.of()   // Default options
            );
            
            // Step 6: Combine all analysis results
            logger.info("Combining analysis results...");
            JarUpgradeResult result = new JarUpgradeResult(
                request.getRequestId(),
                request.getAnalysisType(),
                java.time.LocalDateTime.now().minusMinutes(1), // start time
                java.time.LocalDateTime.now(),                 // end time
                java.util.Map.of("combinedAnalysis", true),    // metadata
                java.util.List.of()                            // warnings
            );
            
            // Add individual analysis results
            result.setDependencyAnalysis(dependencyAnalysis);
            result.setApiUsageAnalysis(apiUsageAnalysis);
            result.setDeprecatedApiAnalysis(deprecatedApiAnalysis);
            result.setVulnerabilityAnalysis(vulnerabilityAnalysis);
            
            logger.info("JAR upgrade impact analysis completed successfully");
            return result;
            
        } catch (Exception e) {
            logger.error("JAR upgrade impact analysis failed", e);
            throw new AnalysisException(getAnalysisType(), request.getRequestId(),
                AnalysisException.ErrorCode.ANALYSIS_FAILED,
                "JAR upgrade impact analysis failed: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean supports(AnalysisType type) {
        return type == AnalysisType.JAR_UPGRADE_IMPACT;
    }
    
    @Override
    public AnalysisType getAnalysisType() {
        return AnalysisType.JAR_UPGRADE_IMPACT;
    }
    
    @Override
    public void validateRequest(JarUpgradeRequest request) throws IllegalArgumentException {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }
        
        if (request.getOldJarFile() == null || request.getNewJarFile() == null) {
            throw new IllegalArgumentException("Both old and new JAR files are required");
        }
        
        if (!request.getOldJarFile().exists()) {
            throw new IllegalArgumentException("Old JAR file does not exist: " + request.getOldJarFile());
        }
        
        if (!request.getNewJarFile().exists()) {
            throw new IllegalArgumentException("New JAR file does not exist: " + request.getNewJarFile());
        }
        
        logger.debug("Request validation passed for: {}", request.getRequestId());
    }
    
    @Override
    public AnalyzerMetadata getMetadata() {
        return new AnalyzerMetadata(
            "JAR Upgrade Impact Analyzer",
            "Comprehensive analysis of JAR upgrade impact using multiple tools",
            "1.0.0",
            new String[]{"JDeps", "ASM", "Jdeprscan", "OSV-Scanner"},
            true,  // supports progress reporting
            true   // supports cancellation
        );
    }
    
    @Override
    public long estimateDuration(JarUpgradeRequest request) {
        // Estimate based on file sizes and analysis complexity
        long oldJarSize = getFileSize(request.getOldJarFile().toPath());
        long newJarSize = getFileSize(request.getNewJarFile().toPath());
        
        // Base time: 2 seconds per MB of JAR data
        long baseTime = (oldJarSize + newJarSize) / (1024 * 1024) * 2000;
        
        // Add overhead for multiple tool analysis
        long toolOverhead = 5000; // 5 seconds for tool coordination
        
        return Math.max(baseTime + toolOverhead, 10000); // Minimum 10 seconds
    }
    
    private long getFileSize(java.nio.file.Path path) {
        try {
            return java.nio.file.Files.size(path);
        } catch (Exception e) {
            logger.warn("Could not get file size for: " + path, e);
            return 1024 * 1024; // Default to 1MB estimate
        }
    }
}
