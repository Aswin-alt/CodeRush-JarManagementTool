package com.coderush.jaranalyzer.common.exception;

import com.coderush.jaranalyzer.common.model.AnalysisType;

/**
 * Base exception for all analysis-related errors.
 * 
 * This exception hierarchy provides structured error handling across all
 * six analysis features, enabling proper error reporting and recovery.
 */
public class AnalysisException extends Exception {
    
    private final AnalysisType analysisType;
    private final String requestId;
    private final ErrorCode errorCode;
    
    /**
     * Error codes for different types of analysis failures.
     */
    public enum ErrorCode {
        // Input-related errors
        INVALID_INPUT("INVALID_INPUT", "Invalid input provided"),
        FILE_NOT_FOUND("FILE_NOT_FOUND", "Required file not found"),
        CORRUPTED_FILE("CORRUPTED_FILE", "File is corrupted or unreadable"),
        UNSUPPORTED_FORMAT("UNSUPPORTED_FORMAT", "Unsupported file format"),
        
        // Processing errors
        ANALYSIS_FAILED("ANALYSIS_FAILED", "Analysis operation failed"),
        TOOL_UNAVAILABLE("TOOL_UNAVAILABLE", "Required analysis tool unavailable"),
        INSUFFICIENT_MEMORY("INSUFFICIENT_MEMORY", "Insufficient memory for analysis"),
        TIMEOUT("TIMEOUT", "Analysis operation timed out"),
        
        // External service errors
        NETWORK_ERROR("NETWORK_ERROR", "Network error during external service call"),
        SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "External service unavailable"),
        API_LIMIT_EXCEEDED("API_LIMIT_EXCEEDED", "External API rate limit exceeded"),
        
        // System errors
        PERMISSION_DENIED("PERMISSION_DENIED", "Insufficient permissions"),
        DISK_SPACE_ERROR("DISK_SPACE_ERROR", "Insufficient disk space"),
        INTERNAL_ERROR("INTERNAL_ERROR", "Internal system error");
        
        private final String code;
        private final String description;
        
        ErrorCode(String code, String description) {
            this.code = code;
            this.description = description;
        }
        
        public String getCode() { return code; }
        public String getDescription() { return description; }
    }
    
    /**
     * Create an analysis exception with error code.
     */
    public AnalysisException(AnalysisType analysisType, String requestId, 
                           ErrorCode errorCode, String message) {
        super(message);
        this.analysisType = analysisType;
        this.requestId = requestId;
        this.errorCode = errorCode;
    }
    
    /**
     * Create an analysis exception with error code and cause.
     */
    public AnalysisException(AnalysisType analysisType, String requestId,
                           ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.analysisType = analysisType;
        this.requestId = requestId;
        this.errorCode = errorCode;
    }
    
    /**
     * Create a simple analysis exception.
     */
    public AnalysisException(AnalysisType analysisType, String message) {
        this(analysisType, null, ErrorCode.ANALYSIS_FAILED, message);
    }
    
    /**
     * Create a simple analysis exception with cause.
     */
    public AnalysisException(AnalysisType analysisType, String message, Throwable cause) {
        this(analysisType, null, ErrorCode.ANALYSIS_FAILED, message, cause);
    }
    
    // Getters
    public AnalysisType getAnalysisType() { return analysisType; }
    public String getRequestId() { return requestId; }
    public ErrorCode getErrorCode() { return errorCode; }
    
    /**
     * Get a user-friendly error message.
     */
    public String getUserMessage() {
        return String.format("%s: %s", 
            errorCode.getDescription(), 
            getMessage());
    }
    
    /**
     * Get a detailed error message for logging.
     */
    public String getDetailedMessage() {
        StringBuilder sb = new StringBuilder();
        sb.append("AnalysisException[");
        sb.append("type=").append(analysisType);
        if (requestId != null) {
            sb.append(", requestId=").append(requestId);
        }
        sb.append(", errorCode=").append(errorCode.getCode());
        sb.append(", message=").append(getMessage());
        sb.append("]");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return getDetailedMessage();
    }
}
