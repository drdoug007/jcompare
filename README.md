# JCompare

JCompare is a professional web-based directory comparison tool designed for Java applications using Apache Maven or Apache Ant. It provides a clean, desktop-like experience for identifying differences between two directory structures and visualizing file-level changes.

## Features

### 📁 Directory Comparison
- **Recursive Comparison**: Analyzes two directories and identifies added, removed, and modified files or subdirectories.
- **Dual Views**:
  - **Tree View**: A hierarchical representation of the directory structure with color-coded status indicators.
  - **Table View**: A sortable/filterable list showing the Path, Type, Status, and detailed modification statistics.
- **Smart Ignoring**: Automatically excludes common project directories like `target`, `.git`, `build`, and `node_modules`. Custom ignore patterns can be configured via a `.jcompare-ignore` file using glob syntax.

### 🔍 Detailed File Differencing
- **Side-by-Side Comparison**: Double-clicking any file in either view opens a side-by-side comparison in a modal dialog.
- **Syntax Highlighting**: Supports automatic syntax highlighting for Java, XML, JSON, YAML, and Properties files using Prism.js.
- **Change Statistics**: Provides precise metrics for each modified file, including:
  - Percentage of total difference.
  - Number of lines added.
  - Number of lines modified.
  - Number of lines deleted.

### 💻 User Experience
- **Desktop Application Feel**: A fluid, high-resolution UI built with Tailwind CSS that maximizes screen real estate.
- **Integrated Directory Selector**: A custom JTE-based modal for browsing and selecting directories directly within the app.
- **Persistence**: Remembers the last selected directory paths using the browser's local storage.
- **Advanced Filtering**: The Table View includes multi-select filters for both file types and change statuses, working together (logical AND) to refine results.

## Technical Stack

- **Java 25**: Leverages the latest Java language features.
- **Spring Boot 4.0.2**: High-performance backend framework.
- **JTE (Java Template Engine)**: Fast, type-safe server-side rendering.
- **Tailwind CSS**: Modern utility-first CSS framework for a professional look.
- **Prism.js**: Robust client-side syntax highlighting.
- **JUnit 5**: Comprehensive test suite ensuring 100% core logic coverage.

## Configuration

### .jcompare-ignore
Create a `.jcompare-ignore` file in the project root to define glob patterns for files and directories that should be excluded from comparison and selection:
```text
target
.git
build
node_modules
*.class
.DS_Store
```

## Getting Started

### Prerequisites
- JDK 25 or higher.
- Maven (or use the provided `./mvnw`).

### Running the Application
1. Clone the repository.
2. Run the application using the Maven wrapper:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Access the application at `http://localhost:8080`.

---
Built with ❤️ using Spring Boot and JTE.
