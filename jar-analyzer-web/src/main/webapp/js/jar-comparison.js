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
        this.currentView = 'summary';
        this.currentResults = null;
        this.filteredChanges = [];
        this.currentDiffIndex = 0;
        
        this.init();
    }
    
    init() {
        this.setupDragAndDrop();
        this.setupFileInputs();
        this.setupEventListeners();
        this.setupViewTabs();
        this.showSection('upload');
    }
    
    /**
     * Setup view tab functionality
     */
    setupViewTabs() {
        const tabButtons = document.querySelectorAll('.tab-button');
        tabButtons.forEach(button => {
            button.addEventListener('click', () => {
                if (button.disabled) return;
                
                const view = button.dataset.view;
                this.switchView(view);
            });
        });
        
        // Setup diff navigation
        const prevBtn = document.getElementById('prevDiffBtn');
        const nextBtn = document.getElementById('nextDiffBtn');
        
        if (prevBtn) {
            prevBtn.addEventListener('click', () => this.navigateDiff(-1));
        }
        if (nextBtn) {
            nextBtn.addEventListener('click', () => this.navigateDiff(1));
        }
        
        // Setup diff options
        const showContextCheckbox = document.getElementById('showContextLines');
        const wordWrapCheckbox = document.getElementById('wordWrapDiff');
        
        if (showContextCheckbox) {
            showContextCheckbox.addEventListener('change', () => this.refreshDiffView());
        }
        if (wordWrapCheckbox) {
            wordWrapCheckbox.addEventListener('change', () => this.toggleWordWrap());
        }
    }
    
    /**
     * Switch between different view modes
     */
    switchView(viewName) {
        console.log('Switching to view:', viewName);
        
        // Update tab buttons
        document.querySelectorAll('.tab-button').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === viewName);
        });
        
        // Update view panels
        document.querySelectorAll('.view-panel').forEach(panel => {
            panel.classList.toggle('active', panel.id === `${viewName}View`);
        });
        
        this.currentView = viewName;
        
        // Refresh the view content if we have results
        if (this.currentResults) {
            console.log('Refreshing view content for:', viewName);
            this.refreshCurrentView();
        }
    }
    
    /**
     * Refresh the current view content
     */
    refreshCurrentView() {
        switch (this.currentView) {
            case 'summary':
                this.renderSummaryView();
                break;
            case 'diff':
                this.renderDiffView();
                break;
            case 'tree':
                // Tree view is not implemented yet
                break;
        }
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
        const compareBtn = document.getElementById('compareBtn');
        if (compareBtn) {
            compareBtn.addEventListener('click', () => {
                this.startComparison();
            });
        }
        
        // New comparison button
        const newComparisonBtn = document.getElementById('newComparisonBtn');
        if (newComparisonBtn) {
            newComparisonBtn.addEventListener('click', () => {
                console.log('New Comparison button clicked!');
                this.resetComparison();
            });
            console.log('New Comparison button event listener added successfully');
        } else {
            console.error('New Comparison button not found! ID: newComparisonBtn');
        }
        
        // Export buttons
        const exportJsonBtn = document.getElementById('exportJsonBtn');
        if (exportJsonBtn) {
            exportJsonBtn.addEventListener('click', () => {
                this.exportResults('json');
            });
        }
        
        const exportHtmlBtn = document.getElementById('exportHtmlBtn');
        if (exportHtmlBtn) {
            exportHtmlBtn.addEventListener('click', () => {
                this.exportResults('html');
            });
        }
        
        // Initialize multiselect dropdowns (if they exist)
        try {
            this.initializeMultiselect();
        } catch (error) {
            console.warn('Could not initialize multiselect dropdowns:', error);
        }
        
        // Search filter (if it exists) - placeholder for future filtering
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.addEventListener('input', () => {
                // TODO: Implement filtering for new view system
                console.log('Search filter triggered:', searchInput.value);
            });
        }
        
        // Clear filters button (if it exists)
        const clearFiltersBtn = document.getElementById('clearFiltersBtn');
        if (clearFiltersBtn) {
            clearFiltersBtn.addEventListener('click', () => {
                this.clearAllFilters();
            });
        }
        
        // Change item expansion AND New Comparison button delegation
        document.addEventListener('click', (e) => {
            // Handle change item expansion
            if (e.target.closest('.change-header')) {
                const changeItem = e.target.closest('.change-item');
                changeItem.classList.toggle('expanded');
            }
            
            // Handle New Comparison button with event delegation
            if (e.target.closest('#newComparisonBtn')) {
                console.log('New Comparison button clicked via event delegation!');
                e.preventDefault();
                e.stopPropagation();
                this.resetComparison();
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
            if (!dropdown) {
                console.warn(`Multiselect element ${elementId} not found`);
                return;
            }
            
            const button = dropdown.querySelector('.multiselect-button');
            const options = dropdown.querySelector('.multiselect-options');
            if (!button || !options) {
                console.warn(`Multiselect components not found for ${elementId}`);
                return;
            }
            
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
                    // TODO: Implement filtering for new view system
                    console.log('Filter changed for', dropdown.id);
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
        const searchInput = document.getElementById('searchInput');
        if (searchInput) {
            searchInput.value = '';
        }
        
        // TODO: Clear filters for new view system if needed
        if (document.getElementById('resultsSection') && 
            document.getElementById('resultsSection').style.display !== 'none') {
            console.log('Clear filters called in results section');
        }
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
     * Display results in the current view mode
     */
    displayResults(results) {
        console.log('Displaying results:', results);
        console.log('Number of changes:', results.changes?.length || 0);
        
        // Store results
        this.currentResults = results;
        this.filteredChanges = results.changes || [];
        
        // Debug log the structure of changes
        if (this.filteredChanges.length > 0) {
            console.log('Sample change structure:', this.filteredChanges[0]);
            console.log('Changes with oldSignature:', this.filteredChanges.filter(c => c.oldSignature).length);
            console.log('Changes with newSignature:', this.filteredChanges.filter(c => c.newSignature).length);
            console.log('Changes with both signatures:', this.filteredChanges.filter(c => c.oldSignature && c.newSignature).length);
        }
        
        // Update header with JAR names
        document.getElementById('oldJarName').textContent = this.files.old.name;
        document.getElementById('newJarName').textContent = this.files.new.name;
        
        // Update summary statistics
        this.updateSummaryStats(results);
        
        // Make sure we start with summary view and all tabs are enabled
        this.enableAllTabs();
        this.switchView('summary');
        
        // Refresh current view
        this.refreshCurrentView();
    }
    
    /**
     * Enable all tab buttons
     */
    enableAllTabs() {
        document.querySelectorAll('.tab-button').forEach(btn => {
            btn.disabled = false;
        });
    }
    
    /**
     * Render the summary view (legacy style)
     */
    renderSummaryView() {
        const container = document.getElementById('summaryContainer');
        const changes = this.filteredChanges;
        
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
        
        container.innerHTML = changes.map(change => this.renderSummaryChangeItem(change)).join('');
        // TODO: Apply filters for new view system if needed
        console.log('Summary view rendered with', changes.length, 'changes');
    }
    
    /**
     * Render the side-by-side diff view
     */
    renderDiffView() {
        const container = document.getElementById('diffContainer');
        const changes = this.filteredChanges.filter(change => {
            // Include changes that have meaningful signatures to compare
            // This includes modifications (both signatures), additions (new signature), and removals (old signature)
            return change.oldSignature || change.newSignature;
        });
        
        console.log('Total filtered changes for diff view:', changes.length);
        console.log('Sample changes:', changes.slice(0, 3));
        
        if (changes.length === 0) {
            container.innerHTML = `
                <div class="no-diffs-message">
                    <i class="fas fa-info-circle"></i>
                    <h3>No Changes Available for Diff View</h3>
                    <p>No changes found with signature data for side-by-side comparison.</p>
                    <p>Try the Summary View to see all detected changes.</p>
                </div>
            `;
            this.updateDiffNavigation(0, 0);
            return;
        }
        
        container.innerHTML = changes.map((change, index) => 
            this.renderDiffItem(change, index)
        ).join('');
        
        this.updateDiffNavigation(changes.length, 1);
        this.currentDiffIndex = 0;
    }
    
    /**
     * Update diff navigation controls
     */
    updateDiffNavigation(total, current) {
        const totalElement = document.getElementById('totalDiffCount');
        const currentElement = document.getElementById('currentDiffIndex');
        const prevBtn = document.getElementById('prevDiffBtn');
        const nextBtn = document.getElementById('nextDiffBtn');
        
        if (totalElement) totalElement.textContent = total;
        if (currentElement) currentElement.textContent = total > 0 ? current : 0;
        
        if (prevBtn) {
            prevBtn.disabled = current <= 1;
        }
        if (nextBtn) {
            nextBtn.disabled = current >= total;
        }
    }
    
    /**
     * Navigate between diffs
     */
    navigateDiff(direction) {
        const diffItems = document.querySelectorAll('.diff-item');
        if (diffItems.length === 0) return;
        
        this.currentDiffIndex = Math.max(0, Math.min(diffItems.length - 1, this.currentDiffIndex + direction));
        
        // Scroll to the diff item
        diffItems[this.currentDiffIndex].scrollIntoView({ 
            behavior: 'smooth', 
            block: 'start' 
        });
        
        this.updateDiffNavigation(diffItems.length, this.currentDiffIndex + 1);
    }
    
    /**
     * Toggle word wrap in diff view
     */
    toggleWordWrap() {
        const wordWrapEnabled = document.getElementById('wordWrapDiff').checked;
        document.querySelectorAll('.diff-code').forEach(code => {
            code.style.wordBreak = wordWrapEnabled ? 'break-word' : 'break-all';
            code.style.whiteSpace = wordWrapEnabled ? 'pre-wrap' : 'pre';
        });
    }
    
    /**
     * Refresh diff view (for context lines toggle)
     */
    refreshDiffView() {
        if (this.currentView === 'diff') {
            this.renderDiffView();
        }
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
     * Render a summary change item (original style)
     */
    renderSummaryChangeItem(change) {
        const typeClass = change.type.toLowerCase();
        const impactClass = change.compatibilityImpact.toLowerCase();
        
        let typeIcon = '';
        switch (change.type) {
            case 'METHOD_ADDED':
            case 'FIELD_ADDED':
            case 'CLASS_ADDED':
                typeIcon = 'fas fa-plus';
                break;
            case 'METHOD_REMOVED':
            case 'FIELD_REMOVED':
            case 'CLASS_REMOVED':
                typeIcon = 'fas fa-minus';
                break;
            default:
                typeIcon = 'fas fa-edit';
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
                    <div class="change-description">${(change.description || 'No description available').trim()}</div>
                    ${this.renderSummaryDiff(change)}
                </div>
            </div>
        `;
    }
    
    /**
     * Render diff for summary view
     */
    renderSummaryDiff(change) {
        if (!change.reasons || change.reasons.length === 0) {
            return '';
        }
        
        const diffLines = change.reasons.map(detail => {
            let lineClass = 'context';
            let prefix = ' ';
            let content = detail;
            
            if (detail.startsWith('- ')) {
                lineClass = 'removed';
                prefix = '-';
                content = detail.substring(2);
            } else if (detail.startsWith('+ ')) {
                lineClass = 'added';
                prefix = '+';
                content = detail.substring(2);
            }
            
            return `<div class="diff-line ${lineClass}">${prefix} ${content}</div>`;
        }).join('');
        
        return `<div class="change-diff">${diffLines}</div>`;
    }
    
    /**
     * Render a single diff item for side-by-side view
     */
    renderDiffItem(change, index) {
        const changeType = this.getChangeTypeInfo(change.type);
        const impactInfo = this.getImpactInfo(change.compatibilityImpact);
        
        return `
            <div class="diff-item" data-index="${index}">
                <div class="diff-header">
                    <div class="diff-title">
                        <i class="${changeType.icon}"></i>
                        <span>${change.className}</span>
                        ${change.memberName ? `<span class="member-name">.${change.memberName}</span>` : ''}
                    </div>
                    <div class="diff-meta">
                        <span class="change-badge ${changeType.class}">${change.type}</span>
                        <span class="impact-badge ${impactInfo.class}">${change.compatibilityImpact}</span>
                    </div>
                </div>
                <div class="side-by-side-diff">
                    <div class="diff-column">
                        <div class="diff-column-header removed">
                            <i class="fas fa-minus-circle"></i>
                            Old Version
                        </div>
                        <div class="diff-code-container">
                            <pre class="diff-code removed">${this.escapeHtml(change.oldSignature || '')}</pre>
                        </div>
                    </div>
                    <div class="diff-column">
                        <div class="diff-column-header added">
                            <i class="fas fa-plus-circle"></i>
                            New Version
                        </div>
                        <div class="diff-code-container">
                            <pre class="diff-code added">${this.escapeHtml(change.newSignature || '')}</pre>
                        </div>
                    </div>
                </div>
                ${change.description ? `<div class="diff-description">${this.escapeHtml(change.description)}</div>` : ''}
                ${change.reasons && change.reasons.length > 0 ? this.renderDiffReasons(change.reasons) : ''}
            </div>
        `;
    }
    
    /**
     * Render reasons list for diff view
     */
    renderDiffReasons(reasons) {
        return `
            <div class="diff-reasons">
                <h5><i class="fas fa-info-circle"></i> Details:</h5>
                <ul>
                    ${reasons.map(reason => `<li>${this.escapeHtml(reason)}</li>`).join('')}
                </ul>
            </div>
        `;
    }
    
    /**
     * Get change type information
     */
    getChangeTypeInfo(type) {
        const types = {
            'METHOD_ADDED': { icon: 'fas fa-plus', class: 'added' },
            'METHOD_REMOVED': { icon: 'fas fa-minus', class: 'removed' },
            'METHOD_ACCESS_CHANGED': { icon: 'fas fa-edit', class: 'modified' },
            'FIELD_ADDED': { icon: 'fas fa-plus', class: 'added' },
            'FIELD_REMOVED': { icon: 'fas fa-minus', class: 'removed' },
            'FIELD_TYPE_CHANGED': { icon: 'fas fa-edit', class: 'modified' },
            'FIELD_ACCESS_CHANGED': { icon: 'fas fa-edit', class: 'modified' },
            'CLASS_ADDED': { icon: 'fas fa-plus', class: 'added' },
            'CLASS_REMOVED': { icon: 'fas fa-minus', class: 'removed' },
            'CLASS_MODIFIED': { icon: 'fas fa-edit', class: 'modified' }
        };
        return types[type] || { icon: 'fas fa-code', class: 'modified' };
    }
    
    /**
     * Get impact information
     */
    getImpactInfo(impact) {
        const impacts = {
            'BREAKING': { class: 'breaking' },
            'LOW': { class: 'low' },
            'NONE': { class: 'none' }
        };
        return impacts[impact] || { class: 'none' };
    }
    
    /**
     * Escape HTML to prevent XSS
     */
    escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
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
        console.log('resetComparison() called - resetting to upload section');
        this.stopProgressMonitoring();
        this.analysisId = null;
        this.files = { old: null, new: null };
        this.currentResults = null;
        
        // Reset file inputs
        const oldJarInput = document.getElementById('oldJarInput');
        const newJarInput = document.getElementById('newJarInput');
        if (oldJarInput) oldJarInput.value = '';
        if (newJarInput) newJarInput.value = '';
        
        // Reset upload areas
        this.removeFile('old');
        this.removeFile('new');
        
        // Reset filters (if they exist)
        const searchInput = document.getElementById('searchInput');
        if (searchInput) searchInput.value = '';
        
        // Clear all multiselect filters
        this.clearAllFilters();
        
        this.showSection('upload');
        console.log('resetComparison() completed - should now show upload section');
    }
    
    /**
     * Show specific section and hide others
     */
    showSection(sectionName) {
        console.log(`showSection('${sectionName}') called`);
        const sections = ['upload', 'progress', 'results'];
        sections.forEach(section => {
            const element = document.getElementById(`${section}Section`);
            if (element) {
                const isVisible = section === sectionName;
                element.style.display = isVisible ? 'block' : 'none';
                console.log(`Section ${section}: ${isVisible ? 'visible' : 'hidden'}`);
            } else {
                console.warn(`Element ${section}Section not found!`);
            }
        });
        console.log(`showSection('${sectionName}') completed`);
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
    
    // Test button existence after DOM is loaded
    console.log('DOM loaded, testing button existence:');
    const testBtn = document.getElementById('newComparisonBtn');
    console.log('newComparisonBtn found:', !!testBtn);
    
    // Add a manual test function to window for debugging
    window.testNewComparisonButton = function() {
        const btn = document.getElementById('newComparisonBtn');
        console.log('Manual test - button found:', !!btn);
        if (btn) {
            console.log('Button visible:', btn.offsetParent !== null);
            console.log('Button onclick:', btn.onclick);
            console.log('Button addEventListener count:', btn.eventListeners?.length || 'unknown');
            // Try to trigger the reset directly
            if (jarComparison) {
                console.log('Calling resetComparison directly...');
                jarComparison.resetComparison();
            }
        }
    };
});

// Global function for home button
function goHome() {
    window.location.href = '/jar-analyzer/';
}
