/**
 * JAR Comparison JavaScript Module
 * Handles file upload, analysis request, progress monitoring, and results display
 */

class JarComparison {
    constructor() {
        this.analysisId = null;
        this.progressInterval = null;
        this.files = {
            old: null,
            new: null
        };
        
        this.init();
    }
    
    init() {
        this.setupDragAndDrop();
        this.setupFileInputs();
        this.setupEventListeners();
        this.showSection('upload');
    }
    
    /**
     * Setup drag and drop functionality for file upload areas
     */
    setupDragAndDrop() {
        const uploadAreas = document.querySelectorAll('.upload-area');
        
        uploadAreas.forEach(area => {
            area.addEventListener('dragover', (e) => {
                e.preventDefault();
                area.classList.add('dragover');
            });
            
            area.addEventListener('dragleave', (e) => {
                e.preventDefault();
                area.classList.remove('dragover');
            });
            
            area.addEventListener('drop', (e) => {
                e.preventDefault();
                area.classList.remove('dragover');
                
                const files = e.dataTransfer.files;
                if (files.length > 0) {
                    const jarType = area.dataset.jarType;
                    this.handleFileSelection(files[0], jarType);
                }
            });
            
            area.addEventListener('click', () => {
                const jarType = area.dataset.jarType;
                const fileInput = document.getElementById(`${jarType}JarInput`);
                fileInput.click();
            });
        });
    }
    
    /**
     * Setup file input change handlers
     */
    setupFileInputs() {
        document.getElementById('oldJarInput').addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelection(e.target.files[0], 'old');
            }
        });
        
        document.getElementById('newJarInput').addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.handleFileSelection(e.target.files[0], 'new');
            }
        });
    }
    
    /**
     * Setup event listeners for buttons and controls
     */
    setupEventListeners() {
        // Compare button
        document.getElementById('compareBtn').addEventListener('click', () => {
            this.startComparison();
        });
        
        // New comparison button
        document.getElementById('newComparisonBtn').addEventListener('click', () => {
            this.resetComparison();
        });
        
        // Export buttons
        document.getElementById('exportJsonBtn').addEventListener('click', () => {
            this.exportResults('json');
        });
        
        document.getElementById('exportHtmlBtn').addEventListener('click', () => {
            this.exportResults('html');
        });
        
        // Initialize multiselect dropdowns
        this.initializeMultiselect();
        
        // Search filter
        document.getElementById('searchInput').addEventListener('input', () => {
            this.applyFilters();
        });
        
        // Clear filters button
        document.getElementById('clearFiltersBtn').addEventListener('click', () => {
            this.clearAllFilters();
        });
        
        // Change item expansion
        document.addEventListener('click', (e) => {
            if (e.target.closest('.change-header')) {
                const changeItem = e.target.closest('.change-item');
                changeItem.classList.toggle('expanded');
            }
        });
    }
    
    /**
     * Initialize multiselect dropdowns
     */
    initializeMultiselect() {
        const multiselectElements = ['changeTypeMultiselect', 'impactMultiselect'];
        
        multiselectElements.forEach(elementId => {
            const dropdown = document.getElementById(elementId);
            const button = dropdown.querySelector('.multiselect-button');
            const options = dropdown.querySelector('.multiselect-options');
            const checkboxes = options.querySelectorAll('input[type="checkbox"]');
            
            // Toggle dropdown on button click
            button.addEventListener('click', (e) => {
                e.stopPropagation();
                this.toggleMultiselect(dropdown);
            });
            
            // Handle keyboard navigation
            button.addEventListener('keydown', (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    this.toggleMultiselect(dropdown);
                }
            });
            
            // Handle checkbox changes
            checkboxes.forEach(checkbox => {
                checkbox.addEventListener('change', () => {
                    // Handle "All" checkbox logic
                    if (checkbox.classList.contains('select-all-checkbox')) {
                        this.handleSelectAll(dropdown, checkbox);
                    } else {
                        this.handleIndividualCheckbox(dropdown, checkbox);
                    }
                    
                    this.updateMultiselectText(dropdown);
                    this.applyFilters();
                });
            });
            
            // Prevent dropdown from closing when clicking inside options
            options.addEventListener('click', (e) => {
                e.stopPropagation();
            });
        });
        
        // Close dropdowns when clicking outside
        document.addEventListener('click', () => {
            this.closeAllMultiselects();
        });
    }
    
    /**
     * Toggle multiselect dropdown open/close
     */
    toggleMultiselect(dropdown) {
        const isOpen = dropdown.classList.contains('open');
        
        // Close all dropdowns first
        this.closeAllMultiselects();
        
        // Toggle the clicked dropdown
        if (!isOpen) {
            dropdown.classList.add('open');
            dropdown.querySelector('.multiselect-button').classList.add('active');
        }
    }
    
    /**
     * Close all multiselect dropdowns
     */
    closeAllMultiselects() {
        const dropdowns = document.querySelectorAll('.multiselect-dropdown');
        dropdowns.forEach(dropdown => {
            dropdown.classList.remove('open');
            dropdown.querySelector('.multiselect-button').classList.remove('active');
        });
    }
    
    /**
     * Update multiselect button text based on selected options
     */
    updateMultiselectText(dropdown) {
        const textElement = dropdown.querySelector('.multiselect-text');
        const selectAllCheckbox = dropdown.querySelector('.select-all-checkbox');
        const otherCheckboxes = dropdown.querySelectorAll('input[type="checkbox"]:not(.select-all-checkbox):checked');
        const totalOtherCheckboxes = dropdown.querySelectorAll('input[type="checkbox"]:not(.select-all-checkbox)');
        const selectedCount = otherCheckboxes.length;
        const totalCount = totalOtherCheckboxes.length;
        
        if (selectAllCheckbox && selectAllCheckbox.checked && selectedCount === totalCount) {
            // "All" is selected and all individual options are selected
            textElement.textContent = 'All';
            textElement.classList.remove('placeholder');
        } else if (selectedCount === 0) {
            // Default placeholder text
            if (dropdown.id === 'changeTypeMultiselect') {
                textElement.textContent = 'Select Change Types';
                textElement.classList.add('placeholder');
            } else if (dropdown.id === 'impactMultiselect') {
                textElement.textContent = 'Select Impact Levels';
                textElement.classList.add('placeholder');
            }
        } else if (selectedCount === 1) {
            // Show single selection
            const selectedLabel = otherCheckboxes[0].getAttribute('data-label');
            textElement.textContent = selectedLabel;
            textElement.classList.remove('placeholder');
        } else {
            // Show count for multiple selections
            textElement.textContent = `${selectedCount} items selected`;
            textElement.classList.remove('placeholder');
        }
    }
    
    /**
     * Get selected values from multiselect dropdown
     */
    getMultiselectValues(dropdownId) {
        const dropdown = document.getElementById(dropdownId);
        const checkboxes = dropdown.querySelectorAll('input[type="checkbox"]:checked:not(.select-all-checkbox)');
        return Array.from(checkboxes).map(cb => cb.value);
    }
    
    /**
     * Handle "All" checkbox selection
     */
    handleSelectAll(dropdown, selectAllCheckbox) {
        const otherCheckboxes = dropdown.querySelectorAll('input[type="checkbox"]:not(.select-all-checkbox)');
        const isChecked = selectAllCheckbox.checked;
        
        // Set all other checkboxes to match the "All" checkbox state
        otherCheckboxes.forEach(checkbox => {
            checkbox.checked = isChecked;
        });
    }
    
    /**
     * Handle individual checkbox selection and update "All" checkbox accordingly
     */
    handleIndividualCheckbox(dropdown, changedCheckbox) {
        const selectAllCheckbox = dropdown.querySelector('.select-all-checkbox');
        const otherCheckboxes = dropdown.querySelectorAll('input[type="checkbox"]:not(.select-all-checkbox)');
        
        // Check if all individual checkboxes are selected
        const allSelected = Array.from(otherCheckboxes).every(checkbox => checkbox.checked);
        const noneSelected = Array.from(otherCheckboxes).every(checkbox => !checkbox.checked);
        
        if (allSelected) {
            // All individual options are selected, check the "All" checkbox
            selectAllCheckbox.checked = true;
            selectAllCheckbox.indeterminate = false;
        } else if (noneSelected) {
            // No individual options are selected, uncheck the "All" checkbox
            selectAllCheckbox.checked = false;
            selectAllCheckbox.indeterminate = false;
        } else {
            // Some but not all options are selected, set "All" to indeterminate
            selectAllCheckbox.checked = false;
            selectAllCheckbox.indeterminate = true;
        }
    }
    
    /**
     * Clear all filters
     */
    clearAllFilters() {
        // Clear multiselect checkboxes
        const checkboxes = document.querySelectorAll('.multiselect-dropdown input[type="checkbox"]');
        checkboxes.forEach(checkbox => {
            checkbox.checked = false;
            checkbox.indeterminate = false;
        });
        
        // Update multiselect texts
        const dropdowns = document.querySelectorAll('.multiselect-dropdown');
        dropdowns.forEach(dropdown => {
            this.updateMultiselectText(dropdown);
        });
        
        // Clear search input
        document.getElementById('searchInput').value = '';
        
        // Reapply filters
        this.applyFilters();
    }
    
    /**
     * Handle file selection and validation
     */
    handleFileSelection(file, jarType) {
        // Validate file type
        if (!file.name.toLowerCase().endsWith('.jar')) {
            this.showNotification('Please select a valid JAR file', 'error');
            return;
        }
        
        // Validate file size (e.g., max 100MB)
        const maxSize = 100 * 1024 * 1024;
        if (file.size > maxSize) {
            this.showNotification('File size too large. Maximum allowed size is 100MB', 'error');
            return;
        }
        
        this.files[jarType] = file;
        this.updateUploadArea(jarType, file);
        this.updateCompareButton();
    }
    
    /**
     * Update upload area UI after file selection
     */
    updateUploadArea(jarType, file) {
        const uploadArea = document.querySelector(`[data-jar-type="${jarType}"]`);
        const uploadBox = uploadArea.closest('.upload-box');
        
        uploadArea.classList.add('has-file');
        
        // Update content
        uploadArea.innerHTML = `
            <div class="upload-icon">
                <i class="fas fa-file-archive"></i>
            </div>
            <h3>JAR File Selected</h3>
            <div class="file-info">
                <i class="fas fa-check-circle"></i>
                <div>
                    <div class="file-name">${file.name}</div>
                    <div class="file-size">${this.formatFileSize(file.size)}</div>
                </div>
                <button class="remove-file" onclick="jarComparison.removeFile('${jarType}')">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `;
    }
    
    /**
     * Remove selected file
     */
    removeFile(jarType) {
        this.files[jarType] = null;
        const uploadArea = document.querySelector(`[data-jar-type="${jarType}"]`);
        
        uploadArea.classList.remove('has-file');
        uploadArea.innerHTML = `
            <div class="upload-icon">
                <i class="fas fa-cloud-upload-alt"></i>
            </div>
            <h3>${jarType === 'old' ? 'Old' : 'New'} JAR File</h3>
            <p>Drag & drop or click to select</p>
        `;
        
        this.updateCompareButton();
    }
    
    /**
     * Update compare button state
     */
    updateCompareButton() {
        const compareBtn = document.getElementById('compareBtn');
        const canCompare = this.files.old && this.files.new;
        
        compareBtn.disabled = !canCompare;
        compareBtn.innerHTML = canCompare 
            ? '<i class="fas fa-code-branch"></i> Compare JARs'
            : '<i class="fas fa-upload"></i> Select both JAR files first';
    }
    
    /**
     * Start the JAR comparison process
     */
    async startComparison() {
        if (!this.files.old || !this.files.new) {
            this.showNotification('Please select both JAR files', 'error');
            return;
        }
        
        try {
            this.showSection('progress');
            this.updateProgress(0, 'Initializing comparison...');
            
            // Prepare form data
            const formData = new FormData();
            formData.append('oldJar', this.files.old);
            formData.append('newJar', this.files.new);
            formData.append('analysisType', 'JAR_COMPARISON');
            
            // Get selected analysis options
            const options = this.getAnalysisOptions();
            formData.append('options', JSON.stringify(options));
            
            // Start analysis
            const response = await fetch('/jar-analyzer/api/analysis/start', {
                method: 'POST',
                body: formData
            });
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const result = await response.json();
            this.analysisId = result.requestId;
            
            this.updateProgress(10, 'Analysis started...');
            this.startProgressMonitoring();
            
        } catch (error) {
            console.error('Error starting comparison:', error);
            this.showNotification('Failed to start comparison: ' + error.message, 'error');
            this.showSection('upload');
        }
    }
    
    /**
     * Get selected analysis options
     */
    getAnalysisOptions() {
        const options = {};
        const checkboxes = document.querySelectorAll('.option-item input[type="checkbox"]');
        
        checkboxes.forEach(checkbox => {
            options[checkbox.value] = checkbox.checked;
        });
        
        return options;
    }
    
    /**
     * Start monitoring analysis progress
     */
    startProgressMonitoring() {
        this.progressInterval = setInterval(async () => {
            try {
                const response = await fetch(`/jar-analyzer/api/analysis/${this.analysisId}/status`);
                
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                
                const status = await response.json();
                
                if (status.status === 'COMPLETED') {
                    this.stopProgressMonitoring();
                    this.loadResults();
                } else if (status.status === 'FAILED') {
                    this.stopProgressMonitoring();
                    this.showNotification('Analysis failed: ' + (status.error || 'Unknown error'), 'error');
                    this.showSection('upload');
                } else {
                    // Update progress
                    const progress = Math.min(90, (status.progress || 0) * 90 / 100);
                    this.updateProgress(progress, status.message || 'Processing...');
                }
                
            } catch (error) {
                console.error('Error checking progress:', error);
                this.stopProgressMonitoring();
                this.showNotification('Failed to check progress: ' + error.message, 'error');
                this.showSection('upload');
            }
        }, 1000);
    }
    
    /**
     * Stop progress monitoring
     */
    stopProgressMonitoring() {
        if (this.progressInterval) {
            clearInterval(this.progressInterval);
            this.progressInterval = null;
        }
    }
    
    /**
     * Update progress bar and message
     */
    updateProgress(percentage, message) {
        document.querySelector('.progress-fill').style.width = `${percentage}%`;
        document.getElementById('progressMessage').textContent = message;
        document.getElementById('progressPercent').textContent = `${Math.round(percentage)}%`;
    }
    
    /**
     * Load and display analysis results
     */
    async loadResults() {
        try {
            this.updateProgress(95, 'Loading results...');
            
            const response = await fetch(`/jar-analyzer/api/analysis/${this.analysisId}/result`);
            
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            
            const results = await response.json();
            this.updateProgress(100, 'Complete!');
            
            // Small delay to show completion
            setTimeout(() => {
                this.displayResults(results);
                this.showSection('results');
            }, 500);
            
        } catch (error) {
            console.error('Error loading results:', error);
            this.showNotification('Failed to load results: ' + error.message, 'error');
            this.showSection('upload');
        }
    }
    
    /**
     * Display comparison results
     */
    displayResults(results) {
        console.log('Displaying results:', results);
        console.log('Number of changes:', results.changes?.length || 0);
        console.log('Results structure check:');
        console.log('- Has changes array:', !!results.changes);
        console.log('- Changes is array:', Array.isArray(results.changes));
        if (results.changes && results.changes.length > 0) {
            console.log('- First change:', results.changes[0]);
        }
        
        // Update header with JAR names
        document.getElementById('oldJarName').textContent = this.files.old.name;
        document.getElementById('newJarName').textContent = this.files.new.name;
        
        // Update summary statistics
        this.updateSummaryStats(results);
        
        // Display changes
        this.displayChanges(results.changes || []);
        
        // Store results for export
        this.currentResults = results;
    }
    
    /**
     * Update summary statistics
     */
    updateSummaryStats(results) {
        const changes = results.changes || [];
        const breakingChanges = changes.filter(change => change.compatibilityImpact === 'BREAKING').length;
        const totalChanges = changes.length;
        const classesAffected = new Set(changes.map(change => change.className)).size;
        const duration = results.durationMs || 0;
        
        document.getElementById('breakingCount').textContent = breakingChanges;
        document.getElementById('totalCount').textContent = totalChanges;
        document.getElementById('classesCount').textContent = classesAffected;
        document.getElementById('durationValue').textContent = this.formatDuration(duration);
    }
    
    /**
     * Display changes in Git-like diff format
     */
    displayChanges(changes) {
        console.log('Displaying changes:', changes.length, 'changes');
        const container = document.getElementById('changesContainer');
        
        if (changes.length === 0) {
            container.innerHTML = `
                <div style="text-align: center; padding: 3rem; color: #666;">
                    <i class="fas fa-check-circle" style="font-size: 3rem; margin-bottom: 1rem; color: #28a745;"></i>
                    <h3>No Differences Found</h3>
                    <p>The JAR files appear to be identical or have no detectable changes.</p>
                </div>
            `;
            return;
        }
        
        container.innerHTML = changes.map(change => this.renderChangeItem(change)).join('');
        console.log('Changes rendered, applying filters...');
        this.applyFilters();
    }
    
    /**
     * Render a single change item
     */
    renderChangeItem(change) {
        const typeClass = change.type.toLowerCase();
        const impactClass = change.compatibilityImpact.toLowerCase();
        
        let typeIcon = '';
        switch (change.type) {
            case 'METHOD_ADDED':
            case 'FIELD_ADDED':
            case 'CLASS_ADDED':
            case 'ADDED':
                typeIcon = 'fas fa-plus';
                break;
            case 'METHOD_REMOVED':
            case 'FIELD_REMOVED':
            case 'CLASS_REMOVED':
            case 'REMOVED':
                typeIcon = 'fas fa-minus';
                break;
            case 'METHOD_MODIFIED':
            case 'FIELD_MODIFIED':
            case 'CLASS_MODIFIED':
            case 'MODIFIED':
                typeIcon = 'fas fa-edit';
                break;
            default:
                typeIcon = 'fas fa-code';
        }
        
        return `
            <div class="change-item" data-type="${change.type}" data-impact="${change.compatibilityImpact}" data-class="${change.className}">
                <div class="change-header">
                    <span class="change-type ${typeClass}">
                        <i class="${typeIcon}"></i>
                        ${change.type}
                    </span>
                    <span class="change-class">${change.className}</span>
                    <span class="change-impact ${impactClass}">${change.compatibilityImpact}</span>
                    <i class="fas fa-chevron-right expand-icon"></i>
                </div>
                <div class="change-details">
                    <div class="change-description">${change.description || 'No description available'}</div>
                    ${this.renderChangeDiff(change)}
                    ${this.renderTechnicalDetails(change)}
                </div>
            </div>
        `;
    }
    
    /**
     * Render change diff in Git-like format
     */
    renderChangeDiff(change) {
        if (!change.reasons || change.reasons.length === 0) {
            return '';
        }
        
        const diffLines = change.reasons.map(detail => {
            let lineClass = 'context';
            let prefix = ' ';
            
            if (detail.startsWith('- ')) {
                lineClass = 'removed';
                prefix = '-';
            } else if (detail.startsWith('+ ')) {
                lineClass = 'added';
                prefix = '+';
            }
            
            return `<div class="diff-line ${lineClass}">${prefix} ${detail.substring(2) || detail}</div>`;
        }).join('');
        
        return `
            <div class="change-diff">
                ${diffLines}
            </div>
        `;
    }
    
    /**
     * Render technical details
     */
    renderTechnicalDetails(change) {
        if (!change.metadata) {
            return '';
        }
        
        const metadata = change.metadata;
        const details = [];
        
        if (metadata.signature) {
            details.push(`Method signature: ${metadata.signature}`);
        }
        if (metadata.visibility) {
            details.push(`Visibility: ${metadata.visibility}`);
        }
        if (metadata.returnType) {
            details.push(`Return type: ${metadata.returnType}`);
        }
        if (metadata.parameters) {
            details.push(`Parameters: ${metadata.parameters.join(', ')}`);
        }
        
        if (details.length === 0) {
            return '';
        }
        
        return `
            <div class="technical-details">
                <h4>Technical Details</h4>
                <ul>
                    ${details.map(detail => `<li>${detail}</li>`).join('')}
                </ul>
            </div>
        `;
    }
    
    /**
     * Apply filters to change list using multiselect values
     */
    applyFilters() {
        // Get selected values from multiselect dropdowns
        const selectedTypes = this.getMultiselectValues('changeTypeMultiselect');
        const selectedImpacts = this.getMultiselectValues('impactMultiselect');
        const searchTerm = document.getElementById('searchInput').value.toLowerCase();
        
        const changeItems = document.querySelectorAll('.change-item');
        console.log('Applying filters to', changeItems.length, 'items');
        console.log('Filters: types=', selectedTypes, 'impacts=', selectedImpacts, 'search=', searchTerm);
        
        let visibleCount = 0;
        
        changeItems.forEach(item => {
            const type = item.dataset.type;
            const impact = item.dataset.impact;
            const className = item.dataset.class.toLowerCase();
            
            let visible = true;
            
            // Apply type filter - show if no types selected OR if item type is in selected types
            if (selectedTypes.length > 0 && !selectedTypes.includes(type)) {
                visible = false;
            }
            
            // Apply impact filter - show if no impacts selected OR if item impact is in selected impacts
            if (selectedImpacts.length > 0 && !selectedImpacts.includes(impact)) {
                visible = false;
            }
            
            // Apply search filter
            if (searchTerm && !className.includes(searchTerm)) {
                visible = false;
            }
            
            item.style.display = visible ? 'block' : 'none';
            if (visible) visibleCount++;
        });
        
        console.log('Visible items after filtering:', visibleCount);
        this.updateFilterSummary(visibleCount, changeItems.length);
    }
    
    /**
     * Update filter summary display
     */
    updateFilterSummary(visibleCount, totalCount) {
        let summaryElement = document.getElementById('filterSummary');
        if (!summaryElement) {
            // Create summary element if it doesn't exist
            const filterControls = document.querySelector('.filter-controls');
            summaryElement = document.createElement('div');
            summaryElement.id = 'filterSummary';
            summaryElement.className = 'filter-summary';
            summaryElement.style.cssText = 'margin-top: 1rem; font-size: 0.9rem; color: #666; text-align: center;';
            filterControls.appendChild(summaryElement);
        }
        
        if (visibleCount === totalCount) {
            summaryElement.textContent = `Showing all ${totalCount} changes`;
        } else {
            summaryElement.textContent = `Showing ${visibleCount} of ${totalCount} changes`;
        }
    }
    
    /**
     * Export results in specified format
     */
    async exportResults(format) {
        if (!this.currentResults) {
            this.showNotification('No results to export', 'error');
            return;
        }
        
        try {
            let content, filename, mimeType;
            
            if (format === 'json') {
                content = JSON.stringify(this.currentResults, null, 2);
                filename = `jar-comparison-${this.getTimestamp()}.json`;
                mimeType = 'application/json';
            } else if (format === 'html') {
                content = this.generateHtmlReport();
                filename = `jar-comparison-${this.getTimestamp()}.html`;
                mimeType = 'text/html';
            }
            
            this.downloadFile(content, filename, mimeType);
            this.showNotification(`Results exported as ${filename}`, 'success');
            
        } catch (error) {
            console.error('Error exporting results:', error);
            this.showNotification('Failed to export results: ' + error.message, 'error');
        }
    }
    
    /**
     * Generate HTML report
     */
    generateHtmlReport() {
        const results = this.currentResults;
        const changes = results.changes || [];
        
        return `
            <!DOCTYPE html>
            <html>
            <head>
                <title>JAR Comparison Report</title>
                <style>
                    body { font-family: Arial, sans-serif; margin: 2rem; }
                    .header { border-bottom: 2px solid #eee; padding-bottom: 1rem; margin-bottom: 2rem; }
                    .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 1rem; margin-bottom: 2rem; }
                    .stat { background: #f8f9fa; padding: 1rem; border-radius: 8px; text-align: center; }
                    .change { border: 1px solid #eee; margin-bottom: 1rem; border-radius: 8px; }
                    .change-header { padding: 1rem; background: #f8f9fa; font-weight: bold; }
                    .change-details { padding: 1rem; }
                    .added { color: #28a745; }
                    .removed { color: #dc3545; }
                    .modified { color: #ffc107; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h1>JAR Comparison Report</h1>
                    <p><strong>Old JAR:</strong> ${this.files.old.name}</p>
                    <p><strong>New JAR:</strong> ${this.files.new.name}</p>
                    <p><strong>Generated:</strong> ${new Date().toLocaleString()}</p>
                </div>
                
                <div class="summary">
                    <div class="stat">
                        <div style="font-size: 2rem; font-weight: bold; color: #dc3545;">${changes.filter(c => c.compatibilityImpact === 'BREAKING').length}</div>
                        <div>Breaking Changes</div>
                    </div>
                    <div class="stat">
                        <div style="font-size: 2rem; font-weight: bold; color: #17a2b8;">${changes.length}</div>
                        <div>Total Changes</div>
                    </div>
                    <div class="stat">
                        <div style="font-size: 2rem; font-weight: bold; color: #28a745;">${new Set(changes.map(c => c.className)).size}</div>
                        <div>Classes Affected</div>
                    </div>
                    <div class="stat">
                        <div style="font-size: 2rem; font-weight: bold; color: #ffc107;">${this.formatDuration(results.durationMs || 0)}</div>
                        <div>Analysis Time</div>
                    </div>
                </div>
                
                <h2>Changes</h2>
                ${changes.map(change => `
                    <div class="change">
                        <div class="change-header ${change.type.toLowerCase()}">
                            ${change.type}: ${change.className} (${change.compatibilityImpact})
                        </div>
                        <div class="change-details">
                            <p>${change.description || 'No description available'}</p>
                            ${change.reasons ? `<pre>${change.reasons.join('\\n')}</pre>` : ''}
                        </div>
                    </div>
                `).join('')}
                
            </body>
            </html>
        `;
    }
    
    /**
     * Download file
     */
    downloadFile(content, filename, mimeType) {
        const blob = new Blob([content], { type: mimeType });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }
    
    /**
     * Reset comparison for new analysis
     */
    resetComparison() {
        this.stopProgressMonitoring();
        this.analysisId = null;
        this.files = { old: null, new: null };
        this.currentResults = null;
        
        // Reset file inputs
        document.getElementById('oldJarInput').value = '';
        document.getElementById('newJarInput').value = '';
        
        // Reset upload areas
        this.removeFile('old');
        this.removeFile('new');
        
        // Reset filters
        document.getElementById('changeTypeFilter').value = '';
        document.getElementById('impactFilter').value = '';
        document.getElementById('searchInput').value = '';
        
        this.showSection('upload');
    }
    
    /**
     * Show specific section and hide others
     */
    showSection(sectionName) {
        const sections = ['upload', 'progress', 'results'];
        sections.forEach(section => {
            const element = document.getElementById(`${section}Section`);
            if (element) {
                element.style.display = section === sectionName ? 'block' : 'none';
            }
        });
    }
    
    /**
     * Show notification to user
     */
    showNotification(message, type = 'info') {
        // Create notification element
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <i class="fas fa-${this.getNotificationIcon(type)}"></i>
                <span>${message}</span>
                <button class="notification-close">&times;</button>
            </div>
        `;
        
        // Add styles
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            z-index: 10000;
            background: ${this.getNotificationColor(type)};
            color: white;
            padding: 1rem;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.15);
            max-width: 400px;
            animation: slideIn 0.3s ease;
        `;
        
        // Add close functionality
        notification.querySelector('.notification-close').addEventListener('click', () => {
            notification.remove();
        });
        
        // Auto remove after 5 seconds
        setTimeout(() => {
            if (notification.parentNode) {
                notification.remove();
            }
        }, 5000);
        
        document.body.appendChild(notification);
    }
    
    /**
     * Get notification icon based on type
     */
    getNotificationIcon(type) {
        switch (type) {
            case 'success': return 'check-circle';
            case 'error': return 'exclamation-circle';
            case 'warning': return 'exclamation-triangle';
            default: return 'info-circle';
        }
    }
    
    /**
     * Get notification color based on type
     */
    getNotificationColor(type) {
        switch (type) {
            case 'success': return '#28a745';
            case 'error': return '#dc3545';
            case 'warning': return '#ffc107';
            default: return '#17a2b8';
        }
    }
    
    /**
     * Format file size for display
     */
    formatFileSize(bytes) {
        if (bytes === 0) return '0 Bytes';
        const k = 1024;
        const sizes = ['Bytes', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
    
    /**
     * Format duration for display
     */
    formatDuration(ms) {
        if (ms < 1000) return `${ms}ms`;
        if (ms < 60000) return `${(ms / 1000).toFixed(1)}s`;
        return `${(ms / 60000).toFixed(1)}m`;
    }
    
    /**
     * Get current timestamp for file naming
     */
    getTimestamp() {
        return new Date().toISOString().replace(/[:.]/g, '-').substring(0, 19);
    }
}

// Add CSS for notifications
const notificationStyles = document.createElement('style');
notificationStyles.textContent = `
    @keyframes slideIn {
        from {
            transform: translateX(100%);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    .notification-content {
        display: flex;
        align-items: center;
        gap: 0.75rem;
    }
    
    .notification-close {
        background: none;
        border: none;
        color: white;
        font-size: 1.2rem;
        cursor: pointer;
        margin-left: auto;
        opacity: 0.8;
    }
    
    .notification-close:hover {
        opacity: 1;
    }
`;
document.head.appendChild(notificationStyles);

// Initialize JAR comparison when DOM is loaded
let jarComparison;
document.addEventListener('DOMContentLoaded', () => {
    jarComparison = new JarComparison();
});

// Global function for home button
function goHome() {
    window.location.href = '/jar-analyzer/';
}
