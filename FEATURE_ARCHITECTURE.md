# CodeRush JAR Management Tool - Feature Architecture

## 🎯 Core Features & Modular Design

This document outlines the six main features and their modular implementation strategy.

## 📊 Feature Overview

### 1. JAR Upgrade Impact Analysis
**Purpose**: Identify all classes/methods in a project that will be affected by upgrading a JAR dependency.

**Inputs**: 
- Project ZIP file
- Old JAR file (current version) 
- New JAR file (target version)

**Process**:
- Extract and analyze project source code
- Compare old vs new JAR APIs using ASM
- Map project code usage to changed APIs
- Generate impact report

**Output**: List of affected classes/methods with impact severity

---

### 2. Unused & Duplicate JAR Detection
**Purpose**: Identify unused dependencies and duplicate JARs in the project's lib folder.

**Inputs**:
- Project ZIP file

**Process**:
- Scan project source code for import statements and class usage
- Analyze JARs in lib folder 
- Cross-reference actual usage vs available JARs
- Detect duplicate JARs by comparing class signatures

**Output**: 
- List of unused JARs (safe to remove)
- List of duplicate JARs with recommendations

---

### 3. Deprecated Method Usage Identification
**Purpose**: Find all usage of deprecated APIs in the project code.

**Inputs**:
- Project ZIP file

**Process**:
- Choose between JDeprscan and ASM based on accuracy and performance
- Scan project bytecode/source for deprecated API usage
- Cross-reference with JDK and dependency deprecation information

**Output**: List of deprecated API usage with replacement suggestions

---

### 4. JAR Comparison
**Purpose**: Detailed comparison between two JAR versions showing all changes.

**Inputs**:
- JAR1 file (lower version)
- JAR2 file (higher version)

**Process**:
- Deep bytecode analysis using ASM
- Compare class structures, method signatures, field definitions
- Identify binary compatibility issues
- Generate comprehensive changeset

**Output**: Detailed changeset with compatibility analysis

---

### 5. Vulnerability Identification
**Purpose**: Identify security vulnerabilities in project dependencies.

**Inputs**:
- Project ZIP file

**Process**:
- Extract dependency information from project
- Query OSV-Scanner API by Google
- Cross-reference with known vulnerability databases
- Generate security report with severity levels

**Output**: Vulnerability report with remediation suggestions

---

### 6. Ant Changes for JAR Upgrade *(Future)*
**Purpose**: Generate Ant build script changes needed for JAR upgrades.

## 🏗️ Modular Architecture Design

### Core Interfaces (jar-analyzer-common)

```java
// Main analyzer interface
public interface ProjectAnalyzer<T extends AnalysisRequest, R extends AnalysisResult> {
    R analyze(T request) throws AnalysisException;
    boolean supports(AnalysisType type);
}

// Feature-specific interfaces
public interface JarUpgradeAnalyzer extends ProjectAnalyzer<JarUpgradeRequest, JarUpgradeResult> {}
public interface UnusedJarAnalyzer extends ProjectAnalyzer<ProjectAnalysisRequest, UnusedJarResult> {}
public interface DeprecatedApiAnalyzer extends ProjectAnalyzer<ProjectAnalysisRequest, DeprecatedApiResult> {}
public interface JarComparisonAnalyzer extends ProjectAnalyzer<JarComparisonRequest, JarComparisonResult> {}
public interface VulnerabilityAnalyzer extends ProjectAnalyzer<ProjectAnalysisRequest, VulnerabilityResult> {}
```

### Implementation Strategy (jar-analyzer-core)

```java
// Package structure for each feature:
com.coderush.jaranalyzer.core.analyzer.upgrade/     # Feature 1
com.coderush.jaranalyzer.core.analyzer.unused/      # Feature 2  
com.coderush.jaranalyzer.core.analyzer.deprecated/  # Feature 3
com.coderush.jaranalyzer.core.analyzer.comparison/  # Feature 4
com.coderush.jaranalyzer.core.analyzer.vulnerability/ # Feature 5

// Shared utilities:
com.coderush.jaranalyzer.core.util.asm/             # ASM utilities
com.coderush.jaranalyzer.core.util.zip/             # ZIP/JAR processing
com.coderush.jaranalyzer.core.util.bytecode/        # Bytecode analysis
com.coderush.jaranalyzer.core.util.dependency/      # Dependency resolution
```

### Web Interface Support (jar-analyzer-web)

```java
// REST endpoints for each feature:
/api/v1/analyze/jar-upgrade        # Feature 1
/api/v1/analyze/unused-jars        # Feature 2
/api/v1/analyze/deprecated-apis    # Feature 3
/api/v1/analyze/jar-comparison     # Feature 4  
/api/v1/analyze/vulnerabilities    # Feature 5

// WebSocket for real-time progress:
/ws/analysis-progress              # All features
```

### CLI Commands (jar-analyzer-cli)

```bash
jar-analyzer upgrade-impact <project.zip> <old.jar> <new.jar>
jar-analyzer unused-jars <project.zip>
jar-analyzer deprecated-apis <project.zip>
jar-analyzer compare-jars <jar1> <jar2>
jar-analyzer vulnerabilities <project.zip>
```

## 🔧 Technical Implementation Strategy

### Input Processing Strategy
- **Unified ZIP Handler**: Single component to handle project ZIP extraction
- **JAR Processing Pipeline**: Consistent JAR analysis across all features
- **Caching Layer**: Cache parsed JAR metadata for performance

### Analysis Engine Strategy
- **ASM-Based Analysis**: Primary tool for bytecode inspection
- **JDeps Integration**: For dependency analysis where appropriate
- **OSV-Scanner Integration**: Dedicated vulnerability scanning
- **Parallel Processing**: Multi-threaded analysis for large projects

### Output Generation Strategy
- **Pluggable Formatters**: JSON, XML, HTML, CSV output formats
- **Template Engine**: Consistent report generation
- **Interactive Reports**: Rich web-based reports with drill-down capability

## 🎯 Why This Modular Design?

### 1. **Feature Independence**
Each feature is implemented as a separate analyzer with its own:
- Input/output models
- Processing logic  
- Error handling
- Testing strategy

### 2. **Shared Infrastructure**
Common functionality is centralized:
- ZIP/JAR processing utilities
- ASM bytecode analysis helpers
- Caching mechanisms
- Progress reporting

### 3. **Extensibility**
Easy to add new features:
- Implement the `ProjectAnalyzer` interface
- Add REST endpoint
- Add CLI command
- Update frontend components

### 4. **Testability**
Each feature can be:
- Unit tested independently
- Integration tested with mock inputs
- Performance tested separately

### 5. **Maintainability**
Clear separation allows:
- Independent development of features
- Focused debugging and optimization
- Gradual feature rollout

## 📈 Development Phases

### Phase 1: Foundation
- Core interfaces and models
- ZIP/JAR processing utilities
- ASM integration helpers

### Phase 2: Core Features (Priority Order)
1. JAR Comparison (simplest, foundational)
2. Unused JAR Detection  
3. JAR Upgrade Impact Analysis
4. Deprecated API Detection
5. Vulnerability Scanning

### Phase 3: Advanced Features
- Rich web interface
- Advanced reporting
- Performance optimizations
- Ant integration (future)

This architecture ensures each feature is:
- **Modular**: Independent implementation
- **Testable**: Clear interfaces for testing
- **Extensible**: Easy to add new capabilities
- **Maintainable**: Focused, single-responsibility components
- **Performant**: Shared optimizations and caching
