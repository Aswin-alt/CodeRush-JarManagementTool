// JAR Management Tool - Main JavaScript

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    loadStats();
});

// Initialize application
function initializeApp() {
    console.log('JAR Management Tool initialized');
    
    // Add keyboard shortcuts
    document.addEventListener('keydown', function(e) {
        if (e.ctrlKey || e.metaKey) {
            switch(e.key) {
                case '1':
                    e.preventDefault();
                    navigateToFeature('jar-comparison');
                    break;
                case '2':
                    e.preventDefault();
                    navigateToFeature('project-analysis');
                    break;
            }
        }
    });
}

// Navigation functions
function navigateToFeature(feature) {
    switch(feature) {
        case 'jar-comparison':
            window.location.href = 'jar-comparison.html';
            break;
        case 'project-analysis':
            window.location.href = 'project-analysis.html';
            break;
        default:
            console.warn('Unknown feature:', feature);
    }
}

// Modal functions
function showComingSoon() {
    const modal = document.getElementById('comingSoonModal');
    modal.style.display = 'block';
    
    // Add animation
    const modalContent = modal.querySelector('.modal-content');
    modalContent.style.transform = 'scale(0.8)';
    modalContent.style.opacity = '0';
    
    setTimeout(() => {
        modalContent.style.transition = 'all 0.3s ease';
        modalContent.style.transform = 'scale(1)';
        modalContent.style.opacity = '1';
    }, 10);
}

function closeModal() {
    const modal = document.getElementById('comingSoonModal');
    const modalContent = modal.querySelector('.modal-content');
    
    modalContent.style.transition = 'all 0.3s ease';
    modalContent.style.transform = 'scale(0.8)';
    modalContent.style.opacity = '0';
    
    setTimeout(() => {
        modal.style.display = 'none';
    }, 300);
}

// Close modal when clicking outside
window.onclick = function(event) {
    const modal = document.getElementById('comingSoonModal');
    if (event.target === modal) {
        closeModal();
    }
}

// Load and display statistics
async function loadStats() {
    try {
        // Try to fetch real stats from the API
        const response = await fetch('/jar-analyzer/api/stats');
        if (response.ok) {
            const stats = await response.json();
            updateStats(stats);
        } else {
            // Use default/demo stats if API is not available
            updateStats({
                totalAnalyses: 0,
                jarComparisons: 0,
                projectScans: 0
            });
        }
    } catch (error) {
        console.warn('Could not load stats:', error);
        // Use demo stats
        updateStats({
            totalAnalyses: 0,
            jarComparisons: 0,
            projectScans: 0
        });
    }
}

// Update statistics display with animation
function updateStats(stats) {
    animateCounter('totalAnalyses', stats.totalAnalyses || 0);
    animateCounter('jarComparisons', stats.jarComparisons || 0);
    animateCounter('projectScans', stats.projectScans || 0);
}

// Animate counter numbers
function animateCounter(elementId, finalValue) {
    const element = document.getElementById(elementId);
    const startValue = 0;
    const duration = 2000; // 2 seconds
    const startTime = performance.now();
    
    function updateCounter(currentTime) {
        const elapsed = currentTime - startTime;
        const progress = Math.min(elapsed / duration, 1);
        
        // Easing function for smooth animation
        const easeOutCubic = 1 - Math.pow(1 - progress, 3);
        const currentValue = Math.floor(startValue + (finalValue - startValue) * easeOutCubic);
        
        element.textContent = currentValue.toLocaleString();
        
        if (progress < 1) {
            requestAnimationFrame(updateCounter);
        }
    }
    
    requestAnimationFrame(updateCounter);
}

// Feature card hover effects
document.addEventListener('DOMContentLoaded', function() {
    const featureCards = document.querySelectorAll('.feature-card:not(.coming-soon)');
    
    featureCards.forEach(card => {
        card.addEventListener('mouseenter', function() {
            this.style.transform = 'translateY(-8px) scale(1.02)';
        });
        
        card.addEventListener('mouseleave', function() {
            this.style.transform = 'translateY(0) scale(1)';
        });
    });
});

// Health check function
async function checkServiceHealth() {
    try {
        const response = await fetch('/jar-analyzer/api/health');
        if (response.ok) {
            const health = await response.json();
            console.log('Service health:', health);
            return true;
        }
    } catch (error) {
        console.warn('Service health check failed:', error);
    }
    return false;
}

// Initialize service check
setTimeout(checkServiceHealth, 1000);
