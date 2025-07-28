package com.coderush.jaranalyzer.common.analyzer;

/**
 * Metadata describing an analyzer's capabilities.
 */
public class AnalyzerMetadata {
    private final String name;
    private final String description;
    private final String version;
    private final String[] requiredTools;
    private final boolean supportsProgressReporting;
    private final boolean supportsCancellation;
    
    public AnalyzerMetadata(String name, String description, String version,
                          String[] requiredTools, boolean supportsProgressReporting,
                          boolean supportsCancellation) {
        this.name = name;
        this.description = description;
        this.version = version;
        this.requiredTools = requiredTools.clone();
        this.supportsProgressReporting = supportsProgressReporting;
        this.supportsCancellation = supportsCancellation;
    }
    
    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getVersion() { return version; }
    public String[] getRequiredTools() { return requiredTools.clone(); }
    public boolean supportsProgressReporting() { return supportsProgressReporting; }
    public boolean supportsCancellation() { return supportsCancellation; }
}
