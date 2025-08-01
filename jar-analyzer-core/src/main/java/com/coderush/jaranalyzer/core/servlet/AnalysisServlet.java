package com.coderush.jaranalyzer.core.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.coderush.jaranalyzer.common.exception.AnalysisException;
import com.coderush.jaranalyzer.common.model.AnalysisResult;
import com.coderush.jaranalyzer.common.model.AnalysisStatus;
import com.coderush.jaranalyzer.common.model.AnalysisType;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonRequest;
import com.coderush.jaranalyzer.common.model.comparison.JarComparisonResult;
import com.coderush.jaranalyzer.core.analyzer.JarComparisonAnalyzer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

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
        
        // Ensure the upload directory exists
        createUploadDirectoryIfNeeded();
        
        logger.info("AnalysisServlet initialized - Ready to handle JAR analysis requests");
        logger.info("Max file upload size: 100MB, Max request size: 500MB");
    }
    
    /**
     * Creates the upload directory if it doesn't exist.
     * This ensures file uploads work even after build/clean operations.
     */
    private void createUploadDirectoryIfNeeded() {
        try {
            // Get the upload location from the multipart config
            String uploadPath = getServletContext().getRealPath("") + "/../../work/Tomcat/localhost/jar-analyzer";
            java.io.File uploadDir = new java.io.File(uploadPath);
            
            if (!uploadDir.exists()) {
                boolean created = uploadDir.mkdirs();
                if (created) {
                    logger.info("Created upload directory: {}", uploadPath);
                } else {
                    logger.warn("Failed to create upload directory: {}", uploadPath);
                }
            } else {
                logger.info("Upload directory already exists: {}", uploadPath);
            }
        } catch (Exception e) {
            logger.error("Error creating upload directory", e);
        }
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
            } else if (pathInfo.startsWith("/")) {
                // Handle /api/analysis/* paths - pathInfo will be like /{analysisId}/status or /{analysisId}/result
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
            } else if (pathInfo.equals("/start")) {
                handleStartAnalysis(request, response);
            } else if (pathInfo.matches("/.+/cancel")) {
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
            throws IOException, ServletException {
        
        logger.info("Starting new analysis request");
        
        try {
            String contentType = request.getContentType();
            Map<String, Object> requestData = new HashMap<>();
            
            // Handle multipart requests (for file uploads like JAR comparison)
            if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data")) {
                logger.info("Processing multipart analysis request");
                
                Collection<Part> parts = request.getParts();
                for (Part part : parts) {
                    if (part.getName() != null) {
                        if ("analysisType".equals(part.getName())) {
                            // Read analysis type from form field
                            String analysisTypeValue = new String(part.getInputStream().readAllBytes());
                            requestData.put("analysisType", analysisTypeValue);
                        } else if ("options".equals(part.getName())) {
                            // Read options from form field
                            String optionsValue = new String(part.getInputStream().readAllBytes());
                            try {
                                Map<String, Object> options = objectMapper.readValue(optionsValue, Map.class);
                                requestData.put("options", options);
                            } catch (Exception e) {
                                logger.warn("Failed to parse options JSON: {}", optionsValue, e);
                                requestData.put("options", new HashMap<>());
                            }
                        } else if (part.getSize() > 0) {
                            // Handle file parts
                            String fileName = getFileName(part);
                            if (fileName != null && !fileName.trim().isEmpty()) {
                                // Store file data directly in request
                                byte[] fileData = part.getInputStream().readAllBytes();
                                requestData.put(part.getName(), fileData);
                                requestData.put(part.getName() + "_filename", fileName);
                                logger.info("Received file: {} ({} bytes)", fileName, fileData.length);
                            }
                        }
                    }
                }
            } else {
                // Handle JSON requests
                String requestBody = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
                requestData = objectMapper.readValue(requestBody, Map.class);
            }
            
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
            result.put("createdAt", context.getCreatedAt());
            
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
        
        // PathInfo will be like "/{analysisId}/status" or "/{analysisId}/result"
        String[] pathParts = pathInfo.split("/");
        if (pathParts.length < 2) {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Invalid analysis path");
            return;
        }
        
        String requestId = pathParts[1]; // First part after the leading slash
        AnalysisRequestContext context = analysisRequests.get(requestId);
        
        if (context == null) {
            sendErrorResponse(response, HttpServletResponse.SC_NOT_FOUND, 
                "Analysis request not found: " + requestId);
            return;
        }
        
        // Check if requesting status or result
        if (pathParts.length > 2 && "status".equals(pathParts[2])) {
            handleGetAnalysisStatus(response, context);
        } else if (pathParts.length > 2 && "result".equals(pathParts[2])) {
            handleGetAnalysisResult(response, context);
        } else {
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, 
                "Invalid analysis endpoint. Use /status or /result");
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
        status.put("createdAt", context.getCreatedAt());
        status.put("progress", context.getCurrentProgress());
        status.put("message", context.getCurrentMessage());
        
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
        
        // PathInfo will be like "/{analysisId}/cancel"
        String[] pathParts = pathInfo.split("/");
        String requestId = pathParts[1]; // First part after the leading slash
        
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
            AnalysisType analysisType = context.getAnalysisType();
            Map<String, Object> requestData = context.getRequestData();
            
            switch (analysisType) {
                case JAR_COMPARISON:
                    return executeJarComparison(context, requestData);
                    
                default:
                    // For other analysis types, return mock result for now
                    Thread.sleep(2000); // Simulate processing time
                    return new MockAnalysisResult(
                        context.getRequestId(),
                        context.getAnalysisType(),
                        "Analysis completed successfully",
                        LocalDateTime.now()
                    );
            }
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(new AnalysisException(context.getAnalysisType(), 
                "Analysis was interrupted", e));
        } catch (Exception e) {
            logger.error("Analysis execution failed for request: {}", context.getRequestId(), e);
            throw new RuntimeException(new AnalysisException(context.getAnalysisType(), 
                "Analysis execution failed: " + e.getMessage(), e));
        }
    }
    
    /**
     * Execute JAR comparison analysis
     */
    private AnalysisResult executeJarComparison(AnalysisRequestContext context, Map<String, Object> requestData) {
        logger.info("Executing JAR comparison for request: {}", context.getRequestId());
        
        try {
            // Extract file data
            byte[] oldJarData = (byte[]) requestData.get("oldJar");
            byte[] newJarData = (byte[]) requestData.get("newJar");
            String oldJarFilename = (String) requestData.get("oldJar_filename");
            String newJarFilename = (String) requestData.get("newJar_filename");
            
            if (oldJarData == null || newJarData == null) {
                throw new AnalysisException(AnalysisType.JAR_COMPARISON, 
                    "Both oldJar and newJar files are required");
            }
            
            // Create temporary files from byte arrays
            java.io.File tempOldJar = createTempFile(oldJarData, oldJarFilename != null ? oldJarFilename : "old.jar");
            java.io.File tempNewJar = createTempFile(newJarData, newJarFilename != null ? newJarFilename : "new.jar");
            
            try {
                // Extract options
                Map<String, Object> options = (Map<String, Object>) requestData.getOrDefault("options", new HashMap<>());
                
                // Create metadata map for the request
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("includePrivateMembers", getBooleanOption(options, "includePrivateMembers", true));
                metadata.put("includePackageClasses", getBooleanOption(options, "includePackageClasses", true));
                metadata.put("analyzeFieldChanges", getBooleanOption(options, "analyzeFieldChanges", true));
                metadata.put("analyzeAnnotations", getBooleanOption(options, "analyzeAnnotations", true));
                
                // Create JAR comparison request
                JarComparisonRequest comparisonRequest = new JarComparisonRequest(
                    tempOldJar,
                    tempNewJar,
                    metadata
                );
                
                // Create analyzer and service
                com.coderush.jaranalyzer.core.service.comparison.impl.AsmJarComparisonService comparisonService = 
                    new com.coderush.jaranalyzer.core.service.comparison.impl.AsmJarComparisonService();
                JarComparisonAnalyzer analyzer = new JarComparisonAnalyzer(comparisonService);
                
                // Execute analysis
                context.setCurrentProgress(10, "Analyzing JAR files...");
                JarComparisonResult result = analyzer.analyze(comparisonRequest);
                context.setCurrentProgress(100, "Analysis complete");
                
                return result;
                
            } finally {
                // Clean up temporary files
                if (tempOldJar.exists()) {
                    tempOldJar.delete();
                }
                if (tempNewJar.exists()) {
                    tempNewJar.delete();
                }
            }
            
        } catch (AnalysisException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.error("JAR comparison failed", e);
            throw new RuntimeException(new AnalysisException(AnalysisType.JAR_COMPARISON, 
                "JAR comparison failed: " + e.getMessage(), e));
        }
    }
    
    /**
     * Create temporary file from byte array
     */
    private java.io.File createTempFile(byte[] data, String originalFilename) throws IOException {
        String prefix = "jar_analysis_";
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        
        java.io.File tempFile = java.io.File.createTempFile(prefix, suffix);
        tempFile.deleteOnExit();
        
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
            fos.write(data);
        }
        
        return tempFile;
    }
    
    /**
     * Extract boolean option from options map
     */
    private boolean getBooleanOption(Map<String, Object> options, String key, boolean defaultValue) {
        Object value = options.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
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
    
    /**
     * Inner class to hold analysis request context
     */
    private static class AnalysisRequestContext {
        private final String requestId;
        private final AnalysisType analysisType;
        private final Map<String, Object> requestData;
        private final LocalDateTime createdAt;
        private AnalysisStatus status;
        private String error;
        private CompletableFuture<AnalysisResult> analysisFuture;
        private int currentProgress = 0;
        private String currentMessage = "Initializing...";
        
        public AnalysisRequestContext(String requestId, AnalysisType analysisType, 
                Map<String, Object> requestData, LocalDateTime createdAt) {
            this.requestId = requestId;
            this.analysisType = analysisType;
            this.requestData = requestData;
            this.createdAt = createdAt;
            this.status = AnalysisStatus.QUEUED;
        }
        
        // Getters
        public String getRequestId() { return requestId; }
        public AnalysisType getAnalysisType() { return analysisType; }
        public Map<String, Object> getRequestData() { return requestData; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public AnalysisStatus getStatus() { return status; }
        public String getError() { return error; }
        public CompletableFuture<AnalysisResult> getAnalysisFuture() { return analysisFuture; }
        public int getCurrentProgress() { return currentProgress; }
        public String getCurrentMessage() { return currentMessage; }
        
        // Setters
        public void setStatus(AnalysisStatus status) { this.status = status; }
        public void setError(String error) { this.error = error; }
        public void setAnalysisFuture(CompletableFuture<AnalysisResult> analysisFuture) { 
            this.analysisFuture = analysisFuture; 
        }
        public void setCurrentProgress(int progress, String message) {
            this.currentProgress = progress;
            this.currentMessage = message != null ? message : this.currentMessage;
        }
    }
    
    /**
     * Mock analysis result for non-implemented analysis types
     */
    private static class MockAnalysisResult extends AnalysisResult {
        public MockAnalysisResult(String requestId, AnalysisType analysisType, 
                String description, LocalDateTime timestamp) {
            super(requestId, analysisType, timestamp, timestamp, new HashMap<>(), new ArrayList<>());
        }
        
        @Override
        public String getSummary() {
            return "Mock analysis completed successfully";
        }
        
        @Override
        public int getTotalFindings() {
            return 0;
        }
    }
}
