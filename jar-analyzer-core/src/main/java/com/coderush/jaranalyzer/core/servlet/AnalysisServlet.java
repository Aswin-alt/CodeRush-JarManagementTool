package com.coderush.jaranalyzer.core.servlet;

import com.coderush.jaranalyzer.common.model.AnalysisType;
import com.coderush.jaranalyzer.common.model.AnalysisRequest;
import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.model.AnalysisStatus;
import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main Analysis Servlet - Entry point for all JAR analysis operations
 * 
 * This servlet handles:
 * - File upload management for JARs and project archives
 * - Analysis request routing to appropriate analyzers
 * - Asynchronous analysis execution with progress tracking
 * - Result retrieval and status monitoring
 * - JSON API responses for the web UI
 * 
 * Why this servlet design:
 * - Centralized request handling with clear routing logic
 * - Asynchronous processing for long-running analysis tasks
 * - Memory-efficient file handling with streaming
 * - Comprehensive error handling and logging
 * - RESTful API design for clean frontend integration
 * - Thread-safe analysis state management
 */
@WebServlet(
    urlPatterns = {"/api/analysis/*", "/api/upload", "/api/features", "/api/health"},
    name = "AnalysisServlet"
)
@MultipartConfig(
    maxFileSize = 100 * 1024 * 1024,        // 100MB max file size
    maxRequestSize = 500 * 1024 * 1024,     // 500MB max request size
    fileSizeThreshold = 10 * 1024 * 1024    // 10MB threshold for temp files
)
public class AnalysisServlet extends HttpServlet {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisServlet.class);
    
    // JSON mapper for request/response serialization
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    
    // Thread pool for asynchronous analysis execution
    private final ExecutorService analysisExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "analysis-worker");
        t.setDaemon(true);
        return t;
    });
    
    // In-memory storage for analysis requests and results
    // In production, this could be replaced with a database or distributed cache
    private final Map<String, AnalysisRequestContext> analysisRequests = new ConcurrentHashMap<>();
    private final Map<String, AnalysisResult> analysisResults = new ConcurrentHashMap<>();
    
    @Override
    public void init() throws ServletException {
        super.init();
        logger.info("AnalysisServlet initialized - Ready to handle JAR analysis requests");
        logger.info("Max file upload size: 100MB, Max request size: 500MB");
    }
    
    @Override
    public void destroy() {
        logger.info("Shutting down AnalysisServlet...");
        analysisExecutor.shutdown();
        super.destroy();
    }
    
    /**
     * Handle GET requests for:
     * - /api/analysis/{id} - Get analysis result
     * - /api/analysis/{id}/status - Get analysis status
     * - /api/features - Get available analysis features
     * - /api/health - Get service health status
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        logger.debug("GET request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                handleGetFeatures(request, response);
            } else if (pathInfo.equals("/features")) {
                handleGetFeatures(request, response);
            } else if (pathInfo.equals("/health")) {
                handleGetHealth(request, response);
            } else if (pathInfo.startsWith("/analysis/")) {
                handleGetAnalysis(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "Endpoint not found: " + pathInfo);
            }
        } catch (Exception e) {
            logger.error("Error handling GET request: " + pathInfo, e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle POST requests for:
     * - /api/upload - File upload endpoint
     * - /api/analysis/start - Start new analysis
     * - /api/analysis/{id}/cancel - Cancel running analysis
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        String pathInfo = request.getPathInfo();
        logger.debug("POST request: {}", pathInfo);
        
        try {
            if (pathInfo == null || pathInfo.equals("/upload")) {
                handleFileUpload(request, response);
            } else if (pathInfo.equals("/analysis/start")) {
                handleStartAnalysis(request, response);
            } else if (pathInfo.matches("/analysis/.+/cancel")) {
                handleCancelAnalysis(request, response, pathInfo);
            } else {
                sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                    "Endpoint not found: " + pathInfo);
            }
        } catch (Exception e) {
            logger.error("Error handling POST request: " + pathInfo, e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Internal server error: " + e.getMessage());
        }
    }
    
    /**
     * Handle file upload requests
     * Processes multipart form data and stores uploaded files temporarily
     */
    private void handleFileUpload(HttpServletRequest request, HttpServletResponse response) 
            throws IOException, ServletException {
        
        logger.info("Processing file upload request");
        
        // Validate content type
        String contentType = request.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("multipart/form-data")) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Request must be multipart/form-data");
            return;
        }
        
        try {
            Collection<Part> parts = request.getParts();
            Map<String, String> uploadedFiles = new HashMap<>();
            
            for (Part part : parts) {
                if (part.getName() != null && part.getSize() > 0) {
                    String fileName = getFileName(part);
                    if (fileName != null && !fileName.trim().isEmpty()) {
                        
                        // Generate unique file ID for tracking
                        String fileId = UUID.randomUUID().toString();
                        
                        // In a real implementation, save file to temporary storage
                        // For now, we'll just track the metadata
                        uploadedFiles.put(part.getName(), fileId);
                        
                        logger.info("Uploaded file: {} ({}), Size: {} bytes", 
                            fileName, fileId, part.getSize());
                    }
                }
            }
            
            // Return upload results
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("uploadedFiles", uploadedFiles);
            result.put("timestamp", LocalDateTime.now());
            
            sendJsonResponse(response, HttpServletResponse.SC_OK, result);
            
        } catch (Exception e) {
            logger.error("File upload failed", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "File upload failed: " + e.getMessage());
        }
    }
    
    /**
     * Handle analysis start requests
     * Creates new analysis request and starts asynchronous processing
     */
    private void handleStartAnalysis(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        logger.info("Starting new analysis request");
        
        try {
            // Parse request body to get analysis parameters
            String requestBody = request.getReader().lines()
                .collect(Collectors.joining(System.lineSeparator()));
            
            Map<String, Object> requestData = objectMapper.readValue(requestBody, Map.class);
            
            // Extract analysis type
            String analysisTypeStr = (String) requestData.get("analysisType");
            if (analysisTypeStr == null) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "analysisType is required");
                return;
            }
            
            AnalysisType analysisType;
            try {
                analysisType = AnalysisType.valueOf(analysisTypeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Invalid analysisType: " + analysisTypeStr);
                return;
            }
            
            // Generate unique request ID
            String requestId = UUID.randomUUID().toString();
            
            // Create analysis context
            AnalysisRequestContext context = new AnalysisRequestContext(
                requestId,
                analysisType,
                requestData,
                LocalDateTime.now()
            );
            
            // Store request context
            analysisRequests.put(requestId, context);
            
            // Start asynchronous analysis
            CompletableFuture<AnalysisResult> analysisFuture = CompletableFuture
                .supplyAsync(() -> executeAnalysis(context), analysisExecutor)
                .whenComplete((result, throwable) -> {
                    if (throwable != null) {
                        logger.error("Analysis failed for request: " + requestId, throwable);
                        context.setStatus(AnalysisStatus.FAILED);
                        context.setError(throwable.getMessage());
                    } else {
                        logger.info("Analysis completed for request: " + requestId);
                        context.setStatus(AnalysisStatus.COMPLETED);
                        analysisResults.put(requestId, result);
                    }
                });
            
            context.setAnalysisFuture(analysisFuture);
            context.setStatus(AnalysisStatus.RUNNING);
            
            // Return request information
            Map<String, Object> result = new HashMap<>();
            result.put("requestId", requestId);
            result.put("analysisType", analysisType);
            result.put("status", AnalysisStatus.RUNNING);
            result.put("startTime", context.getStartTime());
            
            sendJsonResponse(response, HttpServletResponse.SC_ACCEPTED, result);
            
        } catch (Exception e) {
            logger.error("Failed to start analysis", e);
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Failed to start analysis: " + e.getMessage());
        }
    }
    
    /**
     * Handle analysis retrieval requests
     */
    private void handleGetAnalysis(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 3) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Invalid analysis path");
            return;
        }
        
        String requestId = pathParts[2];
        AnalysisRequestContext context = analysisRequests.get(requestId);
        
        if (context == null) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                "Analysis request not found: " + requestId);
            return;
        }
        
        // Check if requesting status or result
        if (pathParts.length > 3 && "status".equals(pathParts[3])) {
            handleGetAnalysisStatus(response, context);
        } else {
            handleGetAnalysisResult(response, context);
        }
    }
    
    /**
     * Handle get analysis status requests
     */
    private void handleGetAnalysisStatus(HttpServletResponse response, AnalysisRequestContext context) 
            throws IOException {
        
        Map<String, Object> status = new HashMap<>();
        status.put("requestId", context.getRequestId());
        status.put("analysisType", context.getAnalysisType());
        status.put("status", context.getStatus());
        status.put("startTime", context.getStartTime());
        
        if (context.getStatus() == AnalysisStatus.FAILED && context.getError() != null) {
            status.put("error", context.getError());
        }
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, status);
    }
    
    /**
     * Handle get analysis result requests
     */
    private void handleGetAnalysisResult(HttpServletResponse response, AnalysisRequestContext context) 
            throws IOException {
        
        if (context.getStatus() == AnalysisStatus.RUNNING) {
            Map<String, Object> result = new HashMap<>();
            result.put("status", "RUNNING");
            result.put("message", "Analysis is still in progress");
            sendJsonResponse(response, HttpServletResponse.SC_ACCEPTED, result);
            return;
        }
        
        if (context.getStatus() == AnalysisStatus.FAILED) {
            sendErrorResponse(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                "Analysis failed: " + context.getError());
            return;
        }
        
        AnalysisResult result = analysisResults.get(context.getRequestId());
        if (result == null) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                "Analysis result not found");
            return;
        }
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, result);
    }
    
    /**
     * Handle cancel analysis requests
     */
    private void handleCancelAnalysis(HttpServletRequest request, HttpServletResponse response, String pathInfo) 
            throws IOException {
        
        String[] pathParts = pathInfo.split("/");
        String requestId = pathParts[2];
        
        AnalysisRequestContext context = analysisRequests.get(requestId);
        if (context == null) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                "Analysis request not found: " + requestId);
            return;
        }
        
        if (context.getAnalysisFuture() != null) {
            boolean cancelled = context.getAnalysisFuture().cancel(true);
            if (cancelled) {
                context.setStatus(AnalysisStatus.CANCELLED);
                logger.info("Analysis cancelled: {}", requestId);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("requestId", requestId);
        result.put("status", context.getStatus());
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, result);
    }
    
    /**
     * Handle get available features requests
     */
    private void handleGetFeatures(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        Map<String, Object> features = new HashMap<>();
        
        for (AnalysisType type : AnalysisType.values()) {
            Map<String, Object> featureInfo = new HashMap<>();
            featureInfo.put("name", type.name());
            featureInfo.put("description", getAnalysisTypeDescription(type));
            featureInfo.put("available", true);
            features.put(type.name().toLowerCase(), featureInfo);
        }
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, features);
    }
    
    /**
     * Handle health check requests
     */
    private void handleGetHealth(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("activeAnalyses", analysisRequests.values().stream()
            .mapToLong(ctx -> ctx.getStatus() == AnalysisStatus.RUNNING ? 1 : 0)
            .sum());
        health.put("totalAnalyses", analysisRequests.size());
        
        sendJsonResponse(response, HttpServletResponse.SC_OK, health);
    }
    
    /**
     * Execute analysis based on request context
     * This is where we'll integrate with the core analysis modules
     */
    private AnalysisResult executeAnalysis(AnalysisRequestContext context) {
        logger.info("Executing analysis: {} for request: {}", 
            context.getAnalysisType(), context.getRequestId());
        
        try {
            // TODO: Integrate with jar-analyzer-core module
            // For now, return a mock result
            Thread.sleep(2000); // Simulate processing time
            
            return new MockAnalysisResult(
                context.getRequestId(),
                context.getAnalysisType(),
                "Analysis completed successfully",
                LocalDateTime.now()
            );
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(new AnalysisException(context.getAnalysisType(), 
                "Analysis was interrupted", e));
        }
    }
    
    /**
     * Utility Methods
     */
    
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        if (contentDisposition != null) {
            for (String token : contentDisposition.split(";")) {
                if (token.trim().startsWith("filename")) {
                    return token.substring(token.indexOf('=') + 1).trim()
                        .replace("\"", "");
                }
            }
        }
        return null;
    }
    
    private String getAnalysisTypeDescription(AnalysisType type) {
        switch (type) {
            case JAR_UPGRADE_IMPACT:
                return "Analyze the impact of upgrading JAR dependencies";
            case UNUSED_JARS:
                return "Detect unused JAR dependencies in your project";
            case DEPRECATED_APIS:
                return "Find deprecated API usage in your codebase";
            case JAR_COMPARISON:
                return "Compare two JAR versions to identify changes";
            case VULNERABILITY_SCAN:
                return "Scan for known security vulnerabilities";
            case ANT_CHANGES:
                return "Generate Ant build script changes for JAR upgrades";
            default:
                return "Analysis feature";
        }
    }
    
    private void sendJsonResponse(HttpServletResponse response, int status, Object data) 
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        try (PrintWriter writer = response.getWriter()) {
            objectMapper.writeValue(writer, data);
        }
    }
    
    private void sendErrorResponse(HttpServletResponse response, int status, String message) 
            throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("message", message);
        error.put("timestamp", LocalDateTime.now());
        
        sendJsonResponse(response, status, error);
    }
}
