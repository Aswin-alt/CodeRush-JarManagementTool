package com.coderush.jaranalyzer.core.servlet;

/**
 * Enum representing the status of an analysis request
 */
public enum AnalysisStatus {
    QUEUED,      // Request received and queued for processing
    RUNNING,     // Analysis is currently executing
    COMPLETED,   // Analysis completed successfully
    FAILED,      // Analysis failed with an error
    CANCELLED    // Analysis was cancelled by user request
}
