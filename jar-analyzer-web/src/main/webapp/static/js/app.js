/**
 * CodeRush JAR Analyzer - Main Application
 * Modern JavaScript ES6+ with modules and async/await
 */

import { ApiClient } from './api.js';
import { FileUploadComponent } from './components/file-upload.js';
import { AnalysisDashboard } from './components/analysis-dashboard.js';
import { WebSocketClient } from './utils/websocket-client.js';

/**
 * Main Application Class
 * 
 * Why this class exists:
 * - Centralized application state management
 * - Coordinated communication between components
 * - Single point of initialization and configuration
 * - Enables modular component architecture
 */
class JarAnalyzerApp {
    constructor() {
        // Initialize core services
        this.apiClient = new ApiClient('/jar-analyzer/api/v1');
        this.wsClient = new WebSocketClient('/jar-analyzer/ws/analysis-progress');
        
        // Component instances
        this.fileUpload = null;
        this.analysisDashboard = null;
        
        // Application state
        this.state = {
            currentAnalysis: null,
            analysisHistory: [],
            uploadedFiles: new Map(),
            activeFeatures: new Set()
        };
        
        // Event listeners cleanup
        this.eventListeners = [];
    }
    
    /**
     * Initialize the application
     * 
     * Why this method exists:
     * - Ensures proper initialization order
     * - Sets up event listeners and components
     * - Handles initial application state
     */
    async init() {
        try {
            console.log('🚀 Initializing CodeRush JAR Analyzer...');
            
            // Initialize components
            await this.initializeComponents();
            
            // Set up global event listeners
            this.setupEventListeners();
            
            // Connect WebSocket for real-time updates
            await this.connectWebSocket();
            
            // Load initial data
            await this.loadInitialData();
            
            console.log('✅ Application initialized successfully');
            
        } catch (error) {
            console.error('❌ Failed to initialize application:', error);
            this.showError('Failed to initialize application. Please refresh the page.');
        }
    }
    
    /**
     * Initialize UI components
     * 
     * Why this method exists:
     * - Modular component initialization
     * - Proper dependency injection
     * - Error handling for component failures
     */
    async initializeComponents() {
        // Initialize file upload component
        const uploadElement = document.getElementById('file-upload-area');
        if (uploadElement) {
            this.fileUpload = new FileUploadComponent(uploadElement, {
                onFilesSelected: (files) => this.handleFilesSelected(files),
                onUploadProgress: (progress) => this.handleUploadProgress(progress),
                onUploadComplete: (result) => this.handleUploadComplete(result),
                onUploadError: (error) => this.handleUploadError(error)
            });
            
            await this.fileUpload.init();
            console.log('📁 File upload component initialized');
        }
        
        // Initialize analysis dashboard
        const dashboardElement = document.getElementById('analysis-grid');
        if (dashboardElement) {
            this.analysisDashboard = new AnalysisDashboard(dashboardElement, {
                onAnalysisStart: (request) => this.handleAnalysisStart(request),
                onAnalysisComplete: (result) => this.handleAnalysisComplete(result),
                onAnalysisError: (error) => this.handleAnalysisError(error),
                apiClient: this.apiClient
            });
            
            await this.analysisDashboard.init();
            console.log('📊 Analysis dashboard initialized');
        }
    }
    
    /**
     * Set up global event listeners
     * 
     * Why this method exists:
     * - Centralized event management
     * - Easy cleanup on destroy
     * - Global keyboard shortcuts and navigation
     */
    setupEventListeners() {
        // Navigation handling
        const navHandler = (e) => {
            if (e.target.classList.contains('nav-link')) {
                e.preventDefault();
                const target = e.target.getAttribute('href').substring(1);
                this.navigateToSection(target);
            }
        };
        
        document.addEventListener('click', navHandler);
        this.eventListeners.push(['click', navHandler]);
        
        // Keyboard shortcuts
        const keyboardHandler = (e) => {
            // Ctrl/Cmd + U: Focus upload area
            if ((e.ctrlKey || e.metaKey) && e.key === 'u') {
                e.preventDefault();
                this.focusUploadArea();
            }
            
            // Escape: Close modals
            if (e.key === 'Escape') {
                this.closeModals();
            }
        };
        
        document.addEventListener('keydown', keyboardHandler);
        this.eventListeners.push(['keydown', keyboardHandler]);
        
        // Handle browser back/forward
        const popstateHandler = (e) => {
            if (e.state && e.state.section) {
                this.navigateToSection(e.state.section, false);
            }
        };
        
        window.addEventListener('popstate', popstateHandler);
        this.eventListeners.push(['popstate', popstateHandler]);
    }
    
    /**
     * Connect WebSocket for real-time updates
     * 
     * Why this method exists:
     * - Real-time progress updates during analysis
     * - Live status updates from server
     * - Enhanced user experience with immediate feedback
     */
    async connectWebSocket() {
        try {
            await this.wsClient.connect();
            
            // Handle progress updates
            this.wsClient.on('analysis-progress', (data) => {
                this.handleProgressUpdate(data);
            });
            
            // Handle analysis completion
            this.wsClient.on('analysis-complete', (data) => {
                this.handleAnalysisComplete(data);
            });
            
            // Handle errors
            this.wsClient.on('error', (error) => {
                console.error('WebSocket error:', error);
            });
            
            console.log('🔌 WebSocket connected');
            
        } catch (error) {
            console.warn('⚠️ WebSocket connection failed, falling back to polling:', error);
            // Fallback to polling if WebSocket fails
            this.startProgressPolling();
        }
    }
    
    /**
     * Load initial application data
     * 
     * Why this method exists:
     * - Restore previous session state
     * - Load available analysis features
     * - Initialize with default values
     */
    async loadInitialData() {
        try {
            // Load available analysis features
            const features = await this.apiClient.getAvailableFeatures();
            this.state.activeFeatures = new Set(features.map(f => f.type));
            
            // Update UI with available features
            this.analysisDashboard?.updateAvailableFeatures(features);
            
            // Load analysis history (last 10 items)
            const history = await this.apiClient.getAnalysisHistory(10);
            this.state.analysisHistory = history;
            
            // Update dashboard with history
            this.analysisDashboard?.updateAnalysisHistory(history);
            
        } catch (error) {
            console.warn('⚠️ Failed to load initial data:', error);
            // Continue with empty state
        }
    }
    
    /**
     * Handle file selection from upload component
     */
    handleFilesSelected(files) {
        console.log(`📎 ${files.length} files selected for upload`);
        
        // Store files in state
        files.forEach(file => {
            this.state.uploadedFiles.set(file.name, {
                file: file,
                uploadedAt: new Date(),
                status: 'pending'
            });
        });
        
        // Update UI
        this.updateFileList();
    }
    
    /**
     * Handle upload progress updates
     */
    handleUploadProgress(progress) {
        this.updateProgressBar(progress.percentage, `Uploading: ${progress.filename}`);
    }
    
    /**
     * Handle successful file upload
     */
    handleUploadComplete(result) {
        console.log('✅ Upload complete:', result);
        
        // Update file status
        if (this.state.uploadedFiles.has(result.filename)) {
            const fileData = this.state.uploadedFiles.get(result.filename);
            fileData.status = 'uploaded';
            fileData.uploadResult = result;
        }
        
        // Enable analysis options
        this.analysisDashboard?.enableAnalysisForFiles([result]);
        
        // Hide progress
        this.hideProgress();
        
        // Show success notification
        this.showSuccess(`File ${result.filename} uploaded successfully`);
    }
    
    /**
     * Handle upload errors
     */
    handleUploadError(error) {
        console.error('❌ Upload error:', error);
        this.hideProgress();
        this.showError(`Upload failed: ${error.message}`);
    }
    
    /**
     * Handle analysis start
     */
    handleAnalysisStart(request) {
        console.log('🔍 Starting analysis:', request);
        
        this.state.currentAnalysis = {
            requestId: request.requestId,
            type: request.analysisType,
            startTime: new Date(),
            status: 'running'
        };
        
        this.showProgress('Initializing analysis...');
    }
    
    /**
     * Handle analysis completion
     */
    handleAnalysisComplete(result) {
        console.log('✅ Analysis complete:', result);
        
        // Update state
        if (this.state.currentAnalysis) {
            this.state.currentAnalysis.status = 'completed';
            this.state.currentAnalysis.result = result;
            this.state.currentAnalysis.endTime = new Date();
        }
        
        // Add to history
        this.state.analysisHistory.unshift(this.state.currentAnalysis);
        
        // Update UI
        this.analysisDashboard?.displayAnalysisResult(result);
        this.hideProgress();
        
        // Show completion notification
        this.showSuccess(`Analysis completed: ${result.summary}`);
        
        // Clear current analysis
        this.state.currentAnalysis = null;
    }
    
    /**
     * Handle analysis errors
     */
    handleAnalysisError(error) {
        console.error('❌ Analysis error:', error);
        
        if (this.state.currentAnalysis) {
            this.state.currentAnalysis.status = 'failed';
            this.state.currentAnalysis.error = error;
        }
        
        this.hideProgress();
        this.showError(`Analysis failed: ${error.message}`);
    }
    
    /**
     * Handle real-time progress updates
     */
    handleProgressUpdate(data) {
        if (data.requestId === this.state.currentAnalysis?.requestId) {
            this.updateProgressBar(data.percentage, data.message);
        }
    }
    
    /**
     * Navigation helper methods
     */
    navigateToSection(sectionId, pushState = true) {
        // Hide all sections
        document.querySelectorAll('.section').forEach(section => {
            section.style.display = 'none';
        });
        
        // Show target section
        const targetSection = document.getElementById(`${sectionId}-section`);
        if (targetSection) {
            targetSection.style.display = 'block';
            
            // Update URL
            if (pushState) {
                history.pushState({ section: sectionId }, '', `#${sectionId}`);
            }
            
            // Update active nav link
            document.querySelectorAll('.nav-link').forEach(link => {
                link.classList.toggle('active', link.getAttribute('href') === `#${sectionId}`);
            });
        }
    }
    
    /**
     * UI Helper Methods
     */
    updateProgressBar(percentage, message) {
        const progressFill = document.getElementById('progress-fill');
        const progressText = document.getElementById('progress-text');
        
        if (progressFill) progressFill.style.width = `${percentage}%`;
        if (progressText) progressText.textContent = message;
    }
    
    showProgress(message) {
        const modal = document.getElementById('progress-modal');
        if (modal) {
            modal.classList.remove('hidden');
            this.updateProgressBar(0, message);
        }
    }
    
    hideProgress() {
        const modal = document.getElementById('progress-modal');
        if (modal) {
            modal.classList.add('hidden');
        }
    }
    
    showSuccess(message) {
        this.showNotification(message, 'success');
    }
    
    showError(message) {
        this.showNotification(message, 'error');
    }
    
    showNotification(message, type) {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.textContent = message;
        
        // Add to document
        document.body.appendChild(notification);
        
        // Auto-remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 5000);
    }
    
    /**
     * Cleanup method
     */
    destroy() {
        // Remove event listeners
        this.eventListeners.forEach(([event, handler]) => {
            document.removeEventListener(event, handler);
        });
        
        // Cleanup components
        this.fileUpload?.destroy();
        this.analysisDashboard?.destroy();
        
        // Close WebSocket
        this.wsClient?.disconnect();
        
        console.log('🧹 Application cleaned up');
    }
}

/**
 * Initialize application when DOM is ready
 * 
 * Why this pattern:
 * - Ensures DOM is fully loaded before initialization
 * - Provides global error handling
 * - Single entry point for the application
 */
document.addEventListener('DOMContentLoaded', async () => {
    try {
        // Create global app instance
        window.jarAnalyzerApp = new JarAnalyzerApp();
        
        // Initialize the application
        await window.jarAnalyzerApp.init();
        
        // Show initial section
        const hash = window.location.hash.substring(1) || 'upload';
        window.jarAnalyzerApp.navigateToSection(hash, false);
        
    } catch (error) {
        console.error('💥 Critical error during app initialization:', error);
        
        // Show fallback error message
        document.body.innerHTML = `
            <div style="padding: 2rem; text-align: center; color: #ef4444;">
                <h1>Application Error</h1>
                <p>Failed to initialize the JAR Analyzer. Please refresh the page.</p>
                <pre style="margin-top: 1rem; text-align: left; background: #f1f5f9; padding: 1rem; border-radius: 0.5rem;">
                    ${error.stack || error.message}
                </pre>
            </div>
        `;
    }
});

// Handle page unload cleanup
window.addEventListener('beforeunload', () => {
    if (window.jarAnalyzerApp) {
        window.jarAnalyzerApp.destroy();
    }
});

export { JarAnalyzerApp };
