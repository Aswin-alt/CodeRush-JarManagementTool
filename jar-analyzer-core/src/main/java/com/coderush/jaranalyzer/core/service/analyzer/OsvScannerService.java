package com.coderush.jaranalyzer.core.service.analyzer;

import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.exception.AnalysisException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service interface for OSV-Scanner-based vulnerability detection.
 * 
 * OSV-Scanner is Google's open-source vulnerability scanner that checks
 * dependencies against the OSV (Open Source Vulnerabilities) database.
 * This service provides:
 * - Known vulnerability detection in JAR dependencies
 * - CVE (Common Vulnerabilities and Exposures) identification
 * - Severity assessment and risk scoring
 * - Remediation recommendations
 * 
 * Why OSV-Scanner is critical for security analysis:
 * 1. Comprehensive vulnerability database (OSV, NVD, GitHub Security Advisory)
 * 2. Regular updates with latest security issues
 * 3. Fast scanning with minimal false positives
 * 4. Supports multiple package ecosystems (Maven, npm, etc.)
 * 5. Industry-standard security tool backed by Google
 * 
 * Security benefits:
 * - Proactive vulnerability identification before deployment
 * - Compliance with security scanning requirements
 * - Risk assessment for dependency upgrades
 * - Integration with CI/CD security gates
 * - Automated security monitoring for JAR dependencies
 */
public interface OsvScannerService {
    
    /**
     * Scan JAR file for known vulnerabilities.
     * 
     * Analyzes the JAR file and its metadata to identify known security
     * vulnerabilities in the OSV database.
     * 
     * @param jarPath Path to the JAR file to scan
     * @param options Scan options (severity filters, database sources, etc.)
     * @return Analysis result containing vulnerability information
     * @throws AnalysisException if scan fails or JAR cannot be processed
     */
    AnalysisResult scanJarForVulnerabilities(Path jarPath, Map<String, Object> options) throws AnalysisException;
    
    /**
     * Scan project dependencies for vulnerabilities.
     * 
     * Performs comprehensive vulnerability scanning across all project
     * dependencies, including transitive dependencies.
     * 
     * @param projectPath Path to the project root directory
     * @param dependencyFiles List of dependency manifest files (pom.xml, build.gradle, etc.)
     * @param options Scan options
     * @return Comprehensive vulnerability analysis for the entire project
     * @throws AnalysisException if scan fails
     */
    AnalysisResult scanProjectForVulnerabilities(Path projectPath, List<Path> dependencyFiles,
                                               Map<String, Object> options) throws AnalysisException;
    
    /**
     * Compare vulnerability profiles between JAR versions.
     * 
     * Analyzes how the vulnerability landscape changes when upgrading
     * from one JAR version to another.
     * 
     * @param oldJarPath Path to the older JAR version
     * @param newJarPath Path to the newer JAR version
     * @param options Comparison options
     * @return Analysis showing vulnerability changes between versions
     * @throws AnalysisException if comparison fails
     */
    AnalysisResult compareVulnerabilityProfiles(Path oldJarPath, Path newJarPath,
                                              Map<String, Object> options) throws AnalysisException;
    
    /**
     * Generate vulnerability remediation report.
     * 
     * Provides specific recommendations for addressing identified
     * vulnerabilities, including version upgrades and workarounds.
     * 
     * @param vulnerabilityResults Previous vulnerability scan results
     * @param options Report generation options
     * @return Detailed remediation recommendations
     * @throws AnalysisException if report generation fails
     */
    AnalysisResult generateRemediationReport(AnalysisResult vulnerabilityResults,
                                           Map<String, Object> options) throws AnalysisException;
    
    /**
     * Filter vulnerabilities by severity level.
     * 
     * Processes vulnerability scan results to focus on specific
     * severity levels (CRITICAL, HIGH, MEDIUM, LOW).
     * 
     * @param vulnerabilityResults Original vulnerability scan results
     * @param minimumSeverity Minimum severity level to include
     * @param options Filtering options
     * @return Filtered vulnerability results
     * @throws AnalysisException if filtering fails
     */
    AnalysisResult filterVulnerabilitiesBySeverity(AnalysisResult vulnerabilityResults,
                                                  String minimumSeverity,
                                                  Map<String, Object> options) throws AnalysisException;
    
    /**
     * Check if OSV-Scanner is available and functional.
     * 
     * @return true if OSV-Scanner is installed and can be executed
     */
    boolean isOsvScannerAvailable();
    
    /**
     * Get the version of OSV-Scanner being used.
     * 
     * @return OSV-Scanner version string
     */
    String getOsvScannerVersion();
    
    /**
     * Update the vulnerability database.
     * 
     * Triggers an update of the local vulnerability database
     * to ensure the latest security information is available.
     * 
     * @return true if database was successfully updated
     * @throws AnalysisException if update fails
     */
    boolean updateVulnerabilityDatabase() throws AnalysisException;
    
    /**
     * Get the last update timestamp of the vulnerability database.
     * 
     * @return Timestamp of the last database update, or null if unknown
     */
    String getLastDatabaseUpdate();
}
