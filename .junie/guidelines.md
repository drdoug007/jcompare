# Guidelines for Junie
## Overview
This Web application compares two directories and shows the differences between them. The directories contain Java Applications, either using apache maven or apache ant build tools.
The application is written in Java 25 and Spring Boot 4.0.2.
The first web page shows the differences between the two directories with a tree view. 

It has a directory read-only textbox for each directory, and a button to select the directories using a directory selection JTE dialog box allowing the user to view the directory structure and select the directory.

The web technical stack is Spring Boot with the JTE template engine.

Double Clicking on a row in either the Tree View or the Table View of the Comparison Results Table with the type file shows the differences in a JTE modal dialog between the two files in a side-by-side view.
Use syntax highlighting to see the differences.

The Table View displays columns for the Path, Type, Status, and for modified files, the percentage, lines added, modified, and deleted differences between the two versions of the file.
The Table View type column should have a specific icon for each file type (Java, XML, JSON, YAML, etc.). The types displayed should be filterable via a dropdown in the column header.
The Table View status column should have a filterable dropdown in the header for each status, such as added, modified, or deleted. Both filters should work together to refine the results.
The Table View should have an export button so the table can be exported to an Excel Spreadsheet. The moved items should have both the source and destination columns.
The application should store the last selected directories in the browser's local storage.
When displayed in a browser, the application should use most of the available screen real estate.

The comparison should ignore project directories such as target, .git, build, node_modules, and any other directories that are not relevant to the comparison. These directories or files ignored should come from a configuration file named `.jcompare-ignore` in the project root, using glob patterns similar to `.gitignore`.
The application should be able to compare directories with a large number of files.



## Coding Guidelines

1) Follow the code style guide
2) Use meaningful variable names
3) Use meaningful comments
4) Use meaningful commit messages
5) Java 25
6) Spring Boot 4.0.2
7) JTE
8) JUnit
9) keep the pom.xml as is.
10) 100% test coverage
11) Commit changes frequently
12) Use tailwind css
13) The application should like a desktop application
14) stop the application gracefully before you make any changes to the codebase
