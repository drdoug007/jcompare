package one.dastec.jcompare.service;

import one.dastec.jcompare.model.DiffNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompareServiceTest {

    private final CompareService compareService = new CompareService();

    @TempDir
    Path tempDir;

    @Test
    void testIdenticalFiles() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left);
        Files.createDirectories(right);

        Files.writeString(left.resolve("file.txt"), "hello");
        Files.writeString(right.resolve("file.txt"), "hello");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.IDENTICAL, result.getStatus());
        assertEquals(1, result.getChildren().size());
        assertEquals(DiffNode.DiffStatus.IDENTICAL, result.getChildren().get(0).getStatus());
    }

    @Test
    void testModifiedFile() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left);
        Files.createDirectories(right);

        Files.writeString(left.resolve("file.txt"), "hello");
        Files.writeString(right.resolve("file.txt"), "world");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.MODIFIED, result.getStatus());
        assertEquals(1, result.getChildren().size());
        assertEquals(DiffNode.DiffStatus.MODIFIED, result.getChildren().get(0).getStatus());
    }

    @Test
    void testAddedFile() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left);
        Files.createDirectories(right);

        Files.writeString(right.resolve("new.txt"), "new content");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.MODIFIED, result.getStatus());
        assertEquals(1, result.getChildren().size());
        assertEquals(DiffNode.DiffStatus.ADDED, result.getChildren().get(0).getStatus());
    }

    @Test
    void testRemovedFile() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left);
        Files.createDirectories(right);

        Files.writeString(left.resolve("old.txt"), "old content");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.MODIFIED, result.getStatus());
        assertEquals(1, result.getChildren().size());
        assertEquals(DiffNode.DiffStatus.REMOVED, result.getChildren().get(0).getStatus());
    }

    @Test
    void testNestedDirectories() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left.resolve("subdir"));
        Files.createDirectories(right.resolve("subdir"));

        Files.writeString(left.resolve("subdir/file.txt"), "hello");
        Files.writeString(right.resolve("subdir/file.txt"), "world");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.MODIFIED, result.getStatus());
        DiffNode subdirNode = result.getChildren().get(0);
        assertEquals("subdir", subdirNode.getName());
        assertEquals(DiffNode.DiffStatus.MODIFIED, subdirNode.getStatus());
        assertEquals(DiffNode.DiffStatus.MODIFIED, subdirNode.getChildren().get(0).getStatus());
    }

    @Test
    void testFlatten() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left.resolve("subdir"));
        Files.createDirectories(right.resolve("subdir"));
        Files.writeString(left.resolve("subdir/file.txt"), "hello");
        Files.writeString(right.resolve("subdir/file.txt"), "world");

        DiffNode result = compareService.compareDirectories(left, right);
        List<CompareService.DiffEntry> entries = compareService.flatten(result);

        assertNotNull(entries);
        assertTrue(entries.size() >= 3); // root, subdir, file.txt
        
        assertTrue(entries.stream().anyMatch(e -> e.path().endsWith("subdir") && e.isDirectory()));
        assertTrue(entries.stream().anyMatch(e -> e.path().endsWith("file.txt") && !e.isDirectory() && e.status() == DiffNode.DiffStatus.MODIFIED));
        assertTrue(entries.stream().anyMatch(e -> !e.relativePath().isEmpty() || e.path().equals(result.getName())));
    }

    @Test
    void testIgnoredDirectories() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left.resolve("target"));
        Files.createDirectories(right.resolve("target"));
        Files.createDirectories(left.resolve(".git"));
        Files.createDirectories(left.resolve("build"));
        Files.createDirectories(left.resolve("node_modules"));
        
        Files.writeString(left.resolve("target/file.txt"), "ignore me");
        Files.writeString(left.resolve("valid.txt"), "keep me");
        Files.writeString(right.resolve("valid.txt"), "keep me");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.IDENTICAL, result.getStatus());
        assertEquals(1, result.getChildren().size());
        assertEquals("valid.txt", result.getChildren().get(0).getName());
    }

    @Test
    void testIgnoredPatterns() throws IOException {
        Path left = tempDir.resolve("left_patterns");
        Path right = tempDir.resolve("right_patterns");
        Files.createDirectories(left);
        Files.createDirectories(right);
        
        Files.writeString(left.resolve("test.class"), "compiled code");
        Files.writeString(left.resolve(".DS_Store"), "metadata");
        Files.writeString(left.resolve("valid.txt"), "keep me");
        Files.writeString(right.resolve("valid.txt"), "keep me");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.IDENTICAL, result.getStatus());
        assertEquals(1, result.getChildren().size());
        assertEquals("valid.txt", result.getChildren().get(0).getName());
    }

    @Test
    void testCompareFiles() throws IOException {
        Path left = tempDir.resolve("file1.txt");
        Path right = tempDir.resolve("file2.txt");

        Files.writeString(left, "line1\nline2\nline3");
        Files.writeString(right, "line1\nlineX\nline3\nline4");

        CompareService.FileDiff diff = compareService.compareFiles(left, right);

        assertEquals(4, diff.lines().size());
        assertEquals(CompareService.LineStatus.IDENTICAL, diff.lines().get(0).status());
        assertEquals(CompareService.LineStatus.MODIFIED, diff.lines().get(1).status());
        assertEquals(CompareService.LineStatus.IDENTICAL, diff.lines().get(2).status());
        assertEquals(CompareService.LineStatus.ADDED, diff.lines().get(3).status());

        assertEquals("line2", diff.lines().get(1).left());
        assertEquals("lineX", diff.lines().get(1).right());
        assertNull(diff.lines().get(3).left());
        assertEquals("line4", diff.lines().get(3).right());

        assertEquals(1, diff.added());
        assertEquals(0, diff.removed());
        assertEquals(1, diff.modified());
        assertEquals(50.0, diff.percentage());
    }

    @Test
    void testCompareFilesRemoved() throws IOException {
        Path left = tempDir.resolve("file1.txt");
        Path right = tempDir.resolve("file2.txt");

        Files.writeString(left, "line1\nline2");
        Files.writeString(right, "line1");

        CompareService.FileDiff diff = compareService.compareFiles(left, right);

        assertEquals(2, diff.lines().size());
        assertEquals(CompareService.LineStatus.REMOVED, diff.lines().get(1).status());
        assertEquals("line2", diff.lines().get(1).left());
        assertNull(diff.lines().get(1).right());

        assertEquals(0, diff.added());
        assertEquals(1, diff.removed());
        assertEquals(0, diff.modified());
        assertEquals(50.0, diff.percentage());
    }

    @Test
    void testCompareFilesOneNull() throws IOException {
        Path right = tempDir.resolve("file2.txt");
        Files.writeString(right, "line1");

        CompareService.FileDiff diff = compareService.compareFiles(null, right);

        assertEquals(1, diff.lines().size());
        assertEquals(CompareService.LineStatus.ADDED, diff.lines().get(0).status());
        assertNull(diff.lines().get(0).left());
        assertEquals("line1", diff.lines().get(0).right());

        assertEquals(1, diff.added());
        assertEquals(0, diff.removed());
        assertEquals(0, diff.modified());
        assertEquals(100.0, diff.percentage());
    }

    @Test
    void testCompareFilesBothEmpty() throws IOException {
        Path left = tempDir.resolve("empty1.txt");
        Path right = tempDir.resolve("empty2.txt");
        Files.createFile(left);
        Files.createFile(right);

        CompareService.FileDiff diff = compareService.compareFiles(left, right);

        assertEquals(0, diff.lines().size());
        assertEquals(0, diff.added());
        assertEquals(0, diff.removed());
        assertEquals(0, diff.modified());
        assertEquals(0.0, diff.percentage());
    }
    @Test
    void testCompareFilesWithEncodingIssue() throws IOException {
        Path file = tempDir.resolve("encoding.txt");
        // Write some bytes that are NOT valid UTF-8
        // For example, 0xFF is not a valid UTF-8 start byte.
        byte[] invalidUtf8 = new byte[] { (byte) 0xFF, (byte) 0xFE, 'A', 'B', 'C' };
        Files.write(file, invalidUtf8);
        // This should not throw MalformedInputException
        CompareService.FileDiff diff = compareService.compareFiles(file, file);
        assertNotNull(diff);
        // It might be empty or contain some weird characters depending on fallback charset,
        // but it should not crash.
    }
    @Test
    void testDeepNestedDirectories() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        String path = "a/b/c/file.txt";
        Path leftFile = left.resolve(path);
        Path rightFile = right.resolve(path);
        Files.createDirectories(leftFile.getParent());
        Files.createDirectories(rightFile.getParent());

        Files.writeString(leftFile, "hello");
        Files.writeString(rightFile, "world");

        DiffNode result = compareService.compareDirectories(left, right);

        assertEquals(DiffNode.DiffStatus.MODIFIED, result.getStatus());
        
        // Find the file node
        DiffNode a = result.getChildren().get(0);
        DiffNode b = a.getChildren().get(0);
        DiffNode c = b.getChildren().get(0);
        DiffNode file = c.getChildren().get(0);
        
        assertEquals("file.txt", file.getName());
        assertEquals("a/b/c/file.txt", file.getRelativePath());
        assertEquals(DiffNode.DiffStatus.MODIFIED, file.getStatus());
    }
}
