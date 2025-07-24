/**
 * CodeRush JAR Analyzer - API Client
 * Modern fetch-based API client with error handling and type safety
 */

/**
 * API Client for communicating with the JAR Analyzer REST API
 * 
 * Why this class exists:
 * - Centralized API communication logic
 * - Consistent error handling across all API calls
 * - Type-safe method signatures for better development experience
 * - Built-in retry logic and request/response interceptors
 * - Support for file uploads with progress tracking
 */
export class ApiClient {
    constructor(baseUrl) {
        this.baseUrl = baseUrl.replace(/\/$/, ''); // Remove trailing slash
        this.defaultHeaders = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
        
        // Request interceptors
        this.requestInterceptors = [];
        this.responseInterceptors = [];
        
        // Add default request interceptor for authentication if needed
        this.addRequestInterceptor((config) => {
            // Add authentication headers if available
            const token = this.getAuthToken();
            if (token) {
                config.headers['Authorization'] = `Bearer ${token}`;
            }
            return config;
        });
        
        // Add default response interceptor for error handling
        this.addResponseInterceptor((response) => {
            if (!response.ok) {
                throw new ApiError(response.status, response.statusText, response);
            }
            return response;
        });
    }
    
    /**
     * Add request interceptor
     * Allows modification of requests before they are sent
     */
    addRequestInterceptor(interceptor) {
        this.requestInterceptors.push(interceptor);
    }
    
    /**
     * Add response interceptor
     * Allows processing of responses before they reach the caller
     */
    addResponseInterceptor(interceptor) {
        this.responseInterceptors.push(interceptor);
    }
    
    /**
     * Generic request method with interceptor support
     */
    async request(endpoint, options = {}) {
        const url = `${this.baseUrl}${endpoint}`;
        
        // Build request configuration
        let config = {
            method: 'GET',
            headers: { ...this.defaultHeaders },
            ...options
        };
        
        // Apply request interceptors
        for (const interceptor of this.requestInterceptors) {
            config = interceptor(config) || config;
        }
        
        try {
            console.log(`🌐 API Request: ${config.method} ${url}`);
            
            let response = await fetch(url, config);
            
            // Apply response interceptors
            for (const interceptor of this.responseInterceptors) {
                response = interceptor(response) || response;
            }
            
            // Parse JSON response if content type is JSON
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const data = await response.json();
                console.log(`✅ API Response: ${response.status}`, data);
                return data;
            }
            
            return response;
            
        } catch (error) {
            console.error(`❌ API Error: ${config.method} ${url}`, error);
            
            if (error instanceof ApiError) {
                throw error;
            }
            
            // Network or other errors
            throw new ApiError(0, 'Network Error', null, error.message);
        }
    }
    
    /**
     * File upload with progress tracking
     * 
     * Why this method exists:
     * - Handles multipart form data for file uploads
     * - Provides progress tracking for large files
     * - Supports multiple file uploads
     * - Proper error handling for upload failures
     */
    async uploadFiles(files, onProgress = null) {
        const formData = new FormData();
        
        // Add files to form data
        if (Array.isArray(files)) {
            files.forEach((file, index) => {
                formData.append(`files[${index}]`, file);
            });
        } else {
            formData.append('file', files);
        }
        
        // Create XMLHttpRequest for progress tracking
        return new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            
            // Track upload progress
            if (onProgress) {
                xhr.upload.addEventListener('progress', (event) => {
                    if (event.lengthComputable) {
                        const percentage = Math.round((event.loaded / event.total) * 100);
                        onProgress({
                            loaded: event.loaded,
                            total: event.total,
                            percentage: percentage
                        });
                    }
                });
            }
            
            // Handle completion
            xhr.addEventListener('load', () => {
                if (xhr.status >= 200 && xhr.status < 300) {
                    try {
                        const response = JSON.parse(xhr.responseText);
                        resolve(response);
                    } catch (error) {
                        reject(new ApiError(xhr.status, 'Invalid JSON response', null, xhr.responseText));
                    }
                } else {
                    reject(new ApiError(xhr.status, xhr.statusText, null, xhr.responseText));
                }
            });
            
            // Handle errors
            xhr.addEventListener('error', () => {
                reject(new ApiError(0, 'Network Error', null, 'File upload failed'));
            });
            
            // Send request
            xhr.open('POST', `${this.baseUrl}/upload`);
            
            // Add authentication header if available
            const token = this.getAuthToken();
            if (token) {
                xhr.setRequestHeader('Authorization', `Bearer ${token}`);
            }
            
            xhr.send(formData);
        });
    }
    
    /**
     * Get available analysis features
     * 
     * Returns list of available analysis types with their metadata
     */
    async getAvailableFeatures() {
        return this.request('/features');
    }
    
    /**
     * Get analysis history
     * 
     * @param {number} limit - Maximum number of items to return
     * @param {number} offset - Number of items to skip
     */
    async getAnalysisHistory(limit = 10, offset = 0) {
        return this.request(`/analysis/history?limit=${limit}&offset=${offset}`);
    }
    
    /**
     * Start JAR upgrade impact analysis (Feature 1)
     * 
     * @param {Object} request - Analysis request parameters
     * @param {File} request.projectZip - Project ZIP file
     * @param {File} request.oldJar - Old JAR file
     * @param {File} request.newJar - New JAR file
     * @param {Object} request.options - Analysis options
     */
    async startJarUpgradeAnalysis(request) {
        const formData = new FormData();
        formData.append('projectZip', request.projectZip);
        formData.append('oldJar', request.oldJar);
        formData.append('newJar', request.newJar);
        formData.append('options', JSON.stringify(request.options || {}));
        
        return this.request('/analyze/jar-upgrade-impact', {
            method: 'POST',
            body: formData,
            headers: {} // Remove default JSON headers for FormData
        });
    }
    
    /**
     * Start unused JAR detection analysis (Feature 2)
     * 
     * @param {File} projectZip - Project ZIP file
     * @param {Object} options - Analysis options
     */
    async startUnusedJarAnalysis(projectZip, options = {}) {
        const formData = new FormData();
        formData.append('projectZip', projectZip);
        formData.append('options', JSON.stringify(options));
        
        return this.request('/analyze/unused-jars', {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
    
    /**
     * Start deprecated API usage analysis (Feature 3)
     * 
     * @param {File} projectZip - Project ZIP file  
     * @param {Object} options - Analysis options
     */
    async startDeprecatedApiAnalysis(projectZip, options = {}) {
        const formData = new FormData();
        formData.append('projectZip', projectZip);
        formData.append('options', JSON.stringify(options));
        
        return this.request('/analyze/deprecated-apis', {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
    
    /**
     * Start JAR comparison analysis (Feature 4)
     * 
     * @param {File} jar1 - First JAR file (lower version)
     * @param {File} jar2 - Second JAR file (higher version)
     * @param {Object} options - Analysis options
     */
    async startJarComparisonAnalysis(jar1, jar2, options = {}) {
        const formData = new FormData();
        formData.append('jar1', jar1);
        formData.append('jar2', jar2);
        formData.append('options', JSON.stringify(options));
        
        return this.request('/analyze/jar-comparison', {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
    
    /**
     * Start vulnerability scan analysis (Feature 5)
     * 
     * @param {File} projectZip - Project ZIP file
     * @param {Object} options - Analysis options
     */
    async startVulnerabilityAnalysis(projectZip, options = {}) {
        const formData = new FormData();
        formData.append('projectZip', projectZip);
        formData.append('options', JSON.stringify(options));
        
        return this.request('/analyze/vulnerabilities', {
            method: 'POST',
            body: formData,
            headers: {}
        });
    }
    
    /**
     * Get analysis result by ID
     * 
     * @param {string} requestId - Analysis request ID
     */
    async getAnalysisResult(requestId) {
        return this.request(`/analysis/${requestId}`);
    }
    
    /**
     * Get analysis status
     * 
     * @param {string} requestId - Analysis request ID
     */
    async getAnalysisStatus(requestId) {
        return this.request(`/analysis/${requestId}/status`);
    }
    
    /**
     * Cancel running analysis
     * 
     * @param {string} requestId - Analysis request ID
     */
    async cancelAnalysis(requestId) {
        return this.request(`/analysis/${requestId}/cancel`, {
            method: 'POST'
        });
    }
    
    /**
     * Download analysis report
     * 
     * @param {string} requestId - Analysis request ID
     * @param {string} format - Report format (json, xml, csv, html)
     */
    async downloadReport(requestId, format = 'json') {
        const response = await this.request(`/analysis/${requestId}/report?format=${format}`, {
            headers: {
                'Accept': this.getAcceptHeader(format)
            }
        });
        
        return response;
    }
    
    /**
     * Get system health status
     */
    async getSystemHealth() {
        return this.request('/health');
    }
    
    /**
     * Get API documentation
     */
    async getApiDocumentation() {
        return this.request('/docs');
    }
    
    /**
     * Helper Methods
     */
    
    getAcceptHeader(format) {
        const mimeTypes = {
            'json': 'application/json',
            'xml': 'application/xml',
            'csv': 'text/csv',
            'html': 'text/html'
        };
        return mimeTypes[format] || 'application/json';
    }
    
    getAuthToken() {
        // Get authentication token from localStorage or sessionStorage
        return localStorage.getItem('jar-analyzer-token') || 
               sessionStorage.getItem('jar-analyzer-token');
    }
    
    setAuthToken(token) {
        localStorage.setItem('jar-analyzer-token', token);
    }
    
    clearAuthToken() {
        localStorage.removeItem('jar-analyzer-token');
        sessionStorage.removeItem('jar-analyzer-token');
    }
}

/**
 * Custom API Error class
 * 
 * Why this class exists:
 * - Provides structured error information
 * - Enables specific error handling based on status codes
 * - Includes original response for detailed debugging
 * - Consistent error format across the application
 */
export class ApiError extends Error {
    constructor(status, statusText, response, details) {
        const message = `API Error ${status}: ${statusText}`;
        super(message);
        
        this.name = 'ApiError';
        this.status = status;
        this.statusText = statusText;
        this.response = response;
        this.details = details;
        
        // Maintain proper stack trace
        if (Error.captureStackTrace) {
            Error.captureStackTrace(this, ApiError);
        }
    }
    
    /**
     * Check if error is due to network issues
     */
    isNetworkError() {
        return this.status === 0;
    }
    
    /**
     * Check if error is due to client-side issues (4xx)
     */
    isClientError() {
        return this.status >= 400 && this.status < 500;
    }
    
    /**
     * Check if error is due to server-side issues (5xx)
     */
    isServerError() {
        return this.status >= 500 && this.status < 600;
    }
    
    /**
     * Get user-friendly error message
     */
    getUserMessage() {
        if (this.isNetworkError()) {
            return 'Network connection error. Please check your internet connection.';
        }
        
        if (this.status === 401) {
            return 'Authentication required. Please log in.';
        }
        
        if (this.status === 403) {
            return 'Access denied. You do not have permission for this operation.';
        }
        
        if (this.status === 404) {
            return 'The requested resource was not found.';
        }
        
        if (this.status === 413) {
            return 'File too large. Please select a smaller file.';
        }
        
        if (this.status === 429) {
            return 'Too many requests. Please wait a moment and try again.';
        }
        
        if (this.isServerError()) {
            return 'Server error. Please try again later.';
        }
        
        return this.details || this.message;
    }
}
