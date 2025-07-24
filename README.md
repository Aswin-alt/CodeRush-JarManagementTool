# CodeRush JAR Management Tool

A comprehensive Java application for analyzing JAR files, inspecting bytecode, detecting vulnerabilities, and generating interactive reports with a modern web interface.

## 🚀 Features

### Core Analysis Capabilities

1. **JAR Upgrade Impact Analysis** 
   - Analyze impact of upgrading JAR dependencies on project code
   - Identify affected classes, methods, and potential breaking changes  
   - Input: Project ZIP + Old JAR + New JAR
   - Output: Detailed impact report with affected code locations

2. **Unused & Duplicate JAR Detection**
   - Identify unused dependencies in project lib folders
   - Detect duplicate JARs with different versions
   - Input: Project ZIP file
   - Output: List of unused/duplicate JARs with removal recommendations

3. **Deprecated API Usage Identification**
   - Find usage of deprecated methods and APIs in project code
   - Use JDeprscan or ASM for comprehensive analysis
   - Input: Project ZIP file  
   - Output: Deprecated API usage locations with replacement suggestions

4. **JAR Version Comparison**
   - Detailed comparison between two JAR versions
   - Identify API changes, binary compatibility issues
   - Input: JAR1 (lower version) + JAR2 (higher version)
   - Output: Comprehensive changeset with compatibility analysis

5. **Vulnerability Identification**
   - Security vulnerability scanning using OSV-Scanner API
   - Identify known security issues in project dependencies
   - Input: Project ZIP file
   - Output: Vulnerability report with severity levels and remediation

6. **Ant Build Changes** *(Future Feature)*
   - Generate Ant build script changes needed for JAR upgrades
   - Automated build configuration updates

### Technical Features
- **Modern Web UI**: Interactive HTML5/CSS3/JavaScript interface
- **REST API**: Full RESTful API for integration with other tools
- **CLI Interface**: Command-line tool for automation and batch processing
- **Real-time Updates**: WebSocket-based progress reporting
- **Multiple Output Formats**: JSON, XML, CSV, HTML reports

## 🏗️ Architecture

This project follows a modular architecture with clear separation of concerns:

### Modules

1. **`jar-analyzer-common`** - Shared utilities, models, and interfaces
2. **`jar-analyzer-core`** - Core analysis engine (JDeps, ASM, Jdeprscan, OSV-Scanner)
3. **`jar-analyzer-web`** - Web interface with REST API and modern UI
4. **`jar-analyzer-cli`** - Command-line interface for automation

### Key Design Principles

- **Feature-Modular Architecture**: Each of the 6 features implemented as independent, testable modules
- **Interface-driven Design**: Consistent `ProjectAnalyzer<T,R>` interface across all features  
- **Extensible Plugin System**: Easy addition of new analysis capabilities
- **Dynamic Input Processing**: Handles various input formats and project structures
- **Configuration Management**: Externalized configuration for different environments
- **Comprehensive Error Handling**: Structured exception hierarchy with graceful degradation
- **Performance Optimized**: Multi-threaded analysis with caching and progress reporting

## 🛠️ Technology Stack

### Backend
- **Java 21** - Latest LTS with modern language features
- **ASM 9.5** - Bytecode manipulation and analysis
- **Jackson** - JSON processing
- **JAX-RS/Jersey** - REST API framework
- **Servlet API** - Web application foundation
- **PicoCLI** - Command-line interface framework

### Frontend
- **HTML5** - Modern semantic markup
- **CSS3** - Advanced styling with Grid/Flexbox
- **Vanilla JavaScript (ES6+)** - No framework dependencies
- **Web Components** - Reusable UI elements
- **WebSocket API** - Real-time communication
- **Progressive Web App** features

### Build & Development
- **Gradle 8.8** - Multi-module build system
- **JUnit 5** - Testing framework
- **Mockito** - Mocking framework
- **Gretty** - Development web server

## 📁 Project Structure

```
CodeRush-JarManagementTool/
├── jar-analyzer-common/           # Shared utilities and models
│   └── src/main/java/com/coderush/jaranalyzer/common/
│       ├── model/                 # Data models and DTOs
│       ├── exception/             # Custom exceptions
│       └── util/                  # Utility classes
├── jar-analyzer-core/             # Core analysis engine
│   └── src/main/java/com/coderush/jaranalyzer/core/
│       ├── analyzer/              # Analysis implementations
│       ├── service/               # Business logic services
│       └── scanner/               # File scanning components
├── jar-analyzer-web/              # Web interface and REST API
│   └── src/main/
│       ├── java/com/coderush/jaranalyzer/web/
│       │   ├── servlet/           # HTTP servlets
│       │   └── rest/              # REST API endpoints
│       └── webapp/
│           ├── static/            # Frontend assets
│           │   ├── css/           # Stylesheets
│           │   ├── js/            # JavaScript modules
│           │   └── assets/        # Images, icons, fonts
│           └── index.html         # Main application page
└── jar-analyzer-cli/              # Command-line interface
    └── src/main/java/com/coderush/jaranalyzer/cli/
        ├── command/               # CLI command implementations
        └── output/                # Output formatters
```

## 🚦 Getting Started

### Prerequisites

- Java 21 or higher
- Gradle 8.8 or higher (included via Gradle Wrapper)

### Building the Project

```bash
# Build all modules
./gradlew build

# Clean and build
./gradlew clean build

# Run tests for all modules
./gradlew test
```

### Running the Web Interface

```bash
# Start the web server (default: http://localhost:8080/jar-analyzer)
./gradlew :jar-analyzer-web:appRun
```

### Running the CLI

```bash
# Build the CLI distribution
./gradlew :jar-analyzer-cli:installDist

# Run the CLI
./jar-analyzer-cli/build/install/jar-analyzer-cli/bin/jar-analyzer-cli --help
```

## 🔧 Development

### Module Dependencies

- `jar-analyzer-common` - Foundation for all other modules
- `jar-analyzer-core` - Depends on `common`
- `jar-analyzer-web` - Depends on `core` and `common`
- `jar-analyzer-cli` - Depends on `core` and `common`

### Adding New Analysis Features

Each of the 6 main features follows this modular implementation pattern:

1. **Define Models** in `jar-analyzer-common`
   - Create `{Feature}Request` extending `AnalysisRequest`
   - Create `{Feature}Result` extending `AnalysisResult`  
   - Add feature-specific data models

2. **Implement Analyzer** in `jar-analyzer-core`
   - Implement `ProjectAnalyzer<{Feature}Request, {Feature}Result>`
   - Add feature-specific processing logic
   - Integrate with tools (ASM, JDeps, OSV-Scanner)

3. **Add REST Endpoints** in `jar-analyzer-web`
   - Create `/api/v1/analyze/{feature-name}` endpoint
   - Add WebSocket support for progress updates
   - Create responsive UI components

4. **Add CLI Commands** in `jar-analyzer-cli`
   - Implement PicoCLI command for the feature
   - Add multiple output format support
   - Enable batch processing capabilities

5. **Testing & Documentation**
   - Unit tests for all components
   - Integration tests with sample data
   - API documentation and user guides

### Configuration

- **Main config**: `gradle.properties`
- **Dependencies**: `gradle/libs.versions.toml`
- **Module configs**: Individual `build.gradle` files

## 🧪 Testing

```bash
# Run all tests
./gradlew testAll

# Run tests for specific module
./gradlew :jar-analyzer-core:test

# Run with test coverage
./gradlew test jacocoTestReport
```

## 📊 Features in Development

The current setup provides the foundation for implementing these features:

### Analysis Capabilities
- JAR dependency analysis using JDeps
- Bytecode inspection using ASM
- Deprecated API detection using Jdeprscan
- Vulnerability scanning using OSV-Scanner
- Class hierarchy analysis
- Method and field usage tracking

### Web Interface Features
- File upload and drag-drop support
- Interactive dependency trees
- Vulnerability reports with severity levels
- Bytecode viewer with syntax highlighting
- Export reports (PDF, JSON, CSV)
- Real-time analysis progress

### CLI Features
- Batch processing multiple JARs
- JSON/XML/CSV output formats
- Integration with CI/CD pipelines
- Configurable analysis rules
- Progress bars and verbose logging

## 🤝 Contributing

This project is designed for extensibility. Key extension points:

- **New Analyzers**: Implement `JarAnalyzer` interface
- **Output Formats**: Add new formatters in CLI module
- **UI Components**: Create new web components
- **REST Endpoints**: Add new API endpoints for custom features

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🏆 Next Steps

1. **Implement Core Analyzers** - Add JDeps, ASM, and OSV-Scanner integration
2. **Build Web Interface** - Create modern, responsive UI components
3. **Add CLI Commands** - Implement comprehensive command-line interface
4. **Testing Suite** - Add comprehensive unit and integration tests
5. **Documentation** - Add detailed API documentation and user guides

---

**Ready for Development!** 🎉

The project structure is now complete and ready for implementing the JAR analysis features. Each module is properly configured with dependencies, and the build system supports development, testing, and deployment workflows.
