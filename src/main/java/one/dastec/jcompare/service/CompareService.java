package one.dastec.jcompare.service;

import one.dastec.jcompare.model.DiffNode;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CompareService {

    private final List<PathMatcher> ignoreMatchers;

    public CompareService() {
        this.ignoreMatchers = loadIgnoreMatchers();
    }

    private List<PathMatcher> loadIgnoreMatchers() {
        List<PathMatcher> matchers = new ArrayList<>();
        Path ignoreFile = Path.of(".jcompare-ignore");
        if (Files.exists(ignoreFile)) {
            try {
                List<String> lines = readAllLines(ignoreFile);
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        // Support basic glob patterns
                        String pattern = line;
                        if (!pattern.contains("/") && !pattern.contains("*") && !pattern.contains("?")) {
                            // If it's just a name, match it anywhere or specifically as a name
                            matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/" + pattern));
                            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
                        } else {
                            if (!pattern.startsWith("**/") && !pattern.startsWith("/")) {
                                matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/" + pattern));
                            }
                            matchers.add(FileSystems.getDefault().getPathMatcher("glob:" + pattern));
                        }
                    }
                }
            } catch (IOException e) {
                // Fallback to defaults if file cannot be read
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/target"));
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/.git"));
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/build"));
                matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/node_modules"));
            }
        } else {
            // Default ignored directories
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/target"));
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/.git"));
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/build"));
            matchers.add(FileSystems.getDefault().getPathMatcher("glob:**/node_modules"));
        }
        return matchers;
    }

    public boolean isIgnored(Path path) {
        return ignoreMatchers.stream().anyMatch(matcher -> matcher.matches(path) || matcher.matches(path.getFileName()));
    }

    public DiffNode compareDirectories(Path left, Path right) throws IOException {
        String rootName = right != null ? right.getFileName().toString() : (left != null ? left.getFileName().toString() : "root");
        DiffNode root = compare(rootName, left, right, "");
        detectMoves(root);
        return root;
    }

    private void detectMoves(DiffNode root) {
        List<DiffNode> addedFiles = new ArrayList<>();
        List<DiffNode> removedFiles = new ArrayList<>();
        collectAddedAndRemovedFiles(root, addedFiles, removedFiles);

        for (DiffNode added : addedFiles) {
            for (DiffNode removed : removedFiles) {
                if (added.getStatus() == DiffNode.DiffStatus.ADDED && removed.getStatus() == DiffNode.DiffStatus.REMOVED) {
                    if (added.getName().equals(removed.getName())) {
                        // For simplicity, just matching by name as per "If a java class exists in a different location"
                        // but we could also check content mismatch here if needed.
                        added.setStatus(DiffNode.DiffStatus.MOVED);
                        added.setSourcePath(removed.getRelativePath());
                        // removed.setStatus(DiffNode.DiffStatus.REMOVED); // Keep as removed for now? 
                        // Or should we mark it as MOVED_FROM? The requirement says "should be displayed as moved and the source should be shown"
                        // Usually this means the new location is marked as MOVED and shows where it came from.
                    }
                }
            }
        }
    }

    private void collectAddedAndRemovedFiles(DiffNode node, List<DiffNode> added, List<DiffNode> removed) {
        if (!node.isDirectory()) {
            if (node.getStatus() == DiffNode.DiffStatus.ADDED) {
                added.add(node);
            } else if (node.getStatus() == DiffNode.DiffStatus.REMOVED) {
                removed.add(node);
            }
        }
        for (DiffNode child : node.getChildren()) {
            collectAddedAndRemovedFiles(child, added, removed);
        }
    }

    public List<DiffEntry> flatten(DiffNode node) {
        List<DiffEntry> entries = new ArrayList<>();
        flatten(node, "", "", entries);
        return entries;
    }

    private void flatten(DiffNode node, String parentPath, String relativePath, List<DiffEntry> entries) {
        String currentPath = parentPath.isEmpty() ? node.getName() : parentPath + "/" + node.getName();
        String currentRelPath;
        if (parentPath.isEmpty()) {
            currentRelPath = ""; // Root
        } else if (relativePath.isEmpty()) {
            currentRelPath = node.getName();
        } else {
            currentRelPath = relativePath + "/" + node.getName();
        }

        entries.add(new DiffEntry(currentPath, node.isDirectory(), node.getStatus(), currentRelPath, node.getAdded(), node.getRemoved(), node.getModified(), node.getPercentage(), node.getSourcePath()));
        for (DiffNode child : node.getChildren()) {
            flatten(child, currentPath, currentRelPath, entries);
        }
    }

    public record DiffEntry(String path, boolean isDirectory, DiffNode.DiffStatus status, String relativePath, int added, int removed, int modified, double percentage, String sourcePath) {}

    public record FileDiff(List<FileDiffLine> lines, int added, int removed, int modified, double percentage) {}

    private List<String> readAllLines(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return Collections.emptyList();
        }
        try {
            return Files.readAllLines(path, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.nio.charset.MalformedInputException e) {
            try {
                return Files.readAllLines(path, java.nio.charset.Charset.defaultCharset());
            } catch (java.nio.charset.MalformedInputException e2) {
                // Fallback to ISO-8859-1 which reads every byte as a character
                return Files.readAllLines(path, java.nio.charset.StandardCharsets.ISO_8859_1);
            }
        }
    }

    public FileDiff compareFiles(Path left, Path right) throws IOException {
        List<String> leftLines = readAllLines(left);
        List<String> rightLines = readAllLines(right);

        List<FileDiffLine> diffLines = new ArrayList<>();
        int maxSize = Math.max(leftLines.size(), rightLines.size());

        int added = 0;
        int removed = 0;
        int modified = 0;
        int identical = 0;

        for (int i = 0; i < maxSize; i++) {
            String leftLine = i < leftLines.size() ? leftLines.get(i) : null;
            String rightLine = i < rightLines.size() ? rightLines.get(i) : null;

            LineStatus status;
            if (leftLine == null) {
                status = LineStatus.ADDED;
                added++;
            } else if (rightLine == null) {
                status = LineStatus.REMOVED;
                removed++;
            } else if (!leftLine.equals(rightLine)) {
                status = LineStatus.MODIFIED;
                modified++;
            } else {
                status = LineStatus.IDENTICAL;
                identical++;
            }

            diffLines.add(new FileDiffLine(leftLine, rightLine, status));
        }

        double percentage = maxSize == 0 ? 0 : (double) (added + removed + modified) / maxSize * 100;

        return new FileDiff(diffLines, added, removed, modified, percentage);
    }

    public record FileDiffLine(String left, String right, LineStatus status) {}

    public enum LineStatus {
        ADDED, REMOVED, MODIFIED, IDENTICAL
    }

    private DiffNode compare(String name, Path left, Path right, String relativePath) throws IOException {
        boolean isDir = (left != null && Files.isDirectory(left)) || (right != null && Files.isDirectory(right));
        
        DiffNode.DiffStatus status;
        if (left == null) {
            status = DiffNode.DiffStatus.ADDED;
        } else if (right == null) {
            status = DiffNode.DiffStatus.REMOVED;
        } else if (isDir) {
            status = DiffNode.DiffStatus.IDENTICAL; // Will be updated if children differ
        } else if (Files.mismatch(left, right) == -1) {
            status = DiffNode.DiffStatus.IDENTICAL;
        } else {
            status = DiffNode.DiffStatus.MODIFIED;
        }

        List<DiffNode> children = new ArrayList<>();
        if (isDir) {
            Set<String> allNames = new TreeSet<>();
            if (left != null && Files.exists(left)) {
                try (Stream<Path> list = Files.list(left)) {
                    list.filter(p -> !isIgnored(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(allNames::add);
                }
            }
            if (right != null && Files.exists(right)) {
                try (Stream<Path> list = Files.list(right)) {
                    list.filter(p -> !isIgnored(p))
                        .map(p -> p.getFileName().toString())
                        .forEach(allNames::add);
                }
            }

            for (String childName : allNames) {
                Path childLeft = left != null ? left.resolve(childName) : null;
                Path childRight = right != null ? right.resolve(childName) : null;
                
                if (childLeft != null && !Files.exists(childLeft)) childLeft = null;
                if (childRight != null && !Files.exists(childRight)) childRight = null;

                String childRelPath = relativePath.isEmpty() ? childName : relativePath + "/" + childName;
                DiffNode childNode = compare(childName, childLeft, childRight, childRelPath);
                children.add(childNode);
                
                if (childNode.getStatus() != DiffNode.DiffStatus.IDENTICAL) {
                    status = DiffNode.DiffStatus.MODIFIED;
                }
            }
        } else {
            // It's a file, calculate stats if modified
            if (status == DiffNode.DiffStatus.MODIFIED || status == DiffNode.DiffStatus.ADDED || status == DiffNode.DiffStatus.REMOVED) {
                FileDiff fileDiff = compareFiles(left, right);
                return DiffNode.builder()
                        .name(name)
                        .isDirectory(false)
                        .status(status)
                        .added(fileDiff.added())
                        .removed(fileDiff.removed())
                        .modified(fileDiff.modified())
                        .percentage(fileDiff.percentage())
                        .relativePath(relativePath)
                        .build();
            }
        }

        return DiffNode.builder()
                .name(name)
                .isDirectory(isDir)
                .status(status)
                .children(children)
                .relativePath(relativePath)
                .build();
    }
}
