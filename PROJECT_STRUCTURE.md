# CodeRush JAR Management Tool - Project Structure

## Overview
This project is designed as a comprehensive JAR analysis tool with a modular architecture that supports multiple interfaces (Web UI, CLI, API) and extensible analysis capabilities.

## Multi-Module Architecture

### 1. `jar-analyzer-common` - Shared Foundation
**Purpose**: Contains shared utilities, models, exceptions, and interfaces used across all modules.

#### Packages:
- `com.coderush.jaranalyzer.common.model` - Data models and DTOs
- `com.coderush.jaranalyzer.common.exception` - Custom exceptions and error handling
- `com.coderush.jaranalyzer.common.util` - Utility classes and helper methods
- `com.coderush.jaranalyzer.common.config` - Configuration management

#### Why this module?
- **DRY Principle**: Eliminates code duplication across modules
- **Consistency**: Ensures uniform data models and error handling
- **Maintainability**: Changes to core models need only be made in one place
- **Extensibility**: New modules can easily depend on shared functionality

### 2. `jar-analyzer-core` - Analysis Engine
**Purpose**: Core analysis logic using JDeps, ASM, Jdeprscan, and OSV-Scanner.

#### Packages:
- `com.coderush.jaranalyzer.core.analyzer` - Analysis engine implementations
  - `JDepsAnalyzer` - Dependency analysis using JDeps
  - `ASMBytecodeAnalyzer` - Bytecode inspection using ASM
  - `DeprecationAnalyzer` - Deprecated API detection using Jdeprscan
  - `VulnerabilityAnalyzer` - Security analysis using OSV-Scanner
- `com.coderush.jaranalyzer.core.service` - Business logic services
- `com.coderush.jaranalyzer.core.scanner` - File scanning and processing
- `com.coderush.jaranalyzer.core.processor` - Processing pipeline components

#### Why this module?
- **Separation of Concerns**: Core logic isolated from UI and CLI
- **Reusability**: Can be used by multiple interfaces (Web, CLI, API)
- **Testability**: Pure business logic is easier to unit test
- **Performance**: Can be optimized independently of UI components

### 3. `jar-analyzer-web` - Web Interface
**Purpose**: Web-based UI with REST API, servlets, and modern frontend.

#### Backend Structure:
- `com.coderush.jaranalyzer.web.servlet` - HTTP servlets for web requests
- `com.coderush.jaranalyzer.web.rest` - REST API endpoints
- `com.coderush.jaranalyzer.web.websocket` - WebSocket handlers for real-time updates
- `com.coderush.jaranalyzer.web.controller` - MVC controllers
- `com.coderush.jaranalyzer.web.filter` - Request/response filters

#### Frontend Structure:
```
webapp/
├── static/
│   ├── css/
│   │   ├── main.css           # Main stylesheet
│   │   ├── components.css     # Component-specific styles
│   │   └── themes/            # Multiple theme support
│   ├── js/
│   │   ├── app.js            # Main application logic
│   │   ├── api.js            # API communication layer
│   │   ├── components/       # Reusable UI components
│   │   │   ├── file-upload.js
│   │   │   ├── analysis-report.js
│   │   │   ├── dependency-tree.js
│   │   │   └── vulnerability-panel.js
│   │   └── utils/            # Utility functions
│   │       ├── dom-utils.js
│   │       ├── chart-utils.js
│   │       └── validation.js
│   └── assets/               # Images, icons, fonts
├── WEB-INF/
│   └── web.xml              # Servlet configuration
└── index.html               # Main application page
```

#### Why this structure?
- **Modern Web Architecture**: Supports both server-side and client-side rendering
- **Component-Based**: Modular frontend components for maintainability
- **API-First**: REST API can be used by other applications
- **Real-time Updates**: WebSocket support for live analysis progress
- **Progressive Enhancement**: Works with JavaScript disabled

### 4. `jar-analyzer-cli` - Command Line Interface
**Purpose**: Command-line interface for batch processing and automation.

#### Packages:
- `com.coderush.jaranalyzer.cli.command` - CLI command implementations
- `com.coderush.jaranalyzer.cli.output` - Output formatters (JSON, XML, CSV, HTML)
- `com.coderush.jaranalyzer.cli.progress` - Progress reporting and logging

#### Why this module?
- **Automation**: Enables CI/CD integration and batch processing
- **Scripting**: Can be used in shell scripts and automated workflows
- **Performance**: No UI overhead for bulk operations
- **Accessibility**: Text-based interface for server environments

## Key Design Principles

### 1. Interface-Driven Design
All major components implement interfaces, allowing for:
- Easy mocking in tests
- Plugin architecture for new analyzers
- Strategy pattern implementation
- Dependency injection

### 2. Extensible Analysis Pipeline
```java
public interface JarAnalyzer {
    AnalysisResult analyze(JarFile jarFile, AnalysisOptions options);
}

public interface VulnerabilityScanner {
    List<Vulnerability> scan(JarMetadata metadata);
}
```

### 3. Configuration Management
- Externalized configuration for different environments
- Plugin discovery mechanism
- Custom analyzer registration

### 4. Error Handling Strategy
- Hierarchical exception handling
- Graceful degradation when tools are unavailable
- Detailed error reporting with context

## Technology Stack

### Backend:
- **Java 21** - Latest LTS with modern language features
- **ASM 9.5** - Bytecode manipulation and analysis
- **Jackson** - JSON processing
- **JAX-RS/Jersey** - REST API framework
- **Servlet API** - Web application foundation
- **PicoCLI** - Command-line interface framework

### Frontend:
- **HTML5** - Modern semantic markup
- **CSS3** - Advanced styling with Grid/Flexbox
- **Vanilla JavaScript (ES6+)** - No framework dependencies
- **Web Components** - For reusable UI elements
- **WebSocket API** - Real-time communication
- **Chart.js** - Data visualization
- **Progressive Web App** features

### Build & Development:
- **Gradle 8.8** - Multi-module build system
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Gretty** - Development web server

## Benefits of This Architecture

### Maintainability:
- Clear separation of concerns
- Modular structure allows focused development
- Consistent coding patterns across modules

### Scalability:
- Each module can be scaled independently
- New analysis tools can be added as plugins
- Web UI can be deployed separately from analysis engine

### Testability:
- Pure business logic in core module
- Interface-based design enables easy mocking
- Each module can be tested in isolation

### Extensibility:
- Plugin architecture for new analyzers
- Multiple output formats supported
- New UI components can be added easily
- API allows third-party integrations

### Performance:
- Core analysis engine optimized for performance
- Web UI uses modern techniques (lazy loading, caching)
- CLI optimized for batch processing

This structure provides a solid foundation for building a comprehensive JAR analysis tool that can grow and adapt to future requirements while maintaining code quality and performance.
