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
    void testFileMoveNoRemovedNode() throws IOException {
        Path left = tempDir.resolve("left_move");
        Path right = tempDir.resolve("right_move");
        Files.createDirectories(left.resolve("dir1"));
        Files.createDirectories(right.resolve("dir2"));

        Files.writeString(left.resolve("dir1/moved.txt"), "same content");
        Files.writeString(right.resolve("dir2/moved.txt"), "same content");

        DiffNode result = compareService.compareDirectories(left, right);

        // Verify the MOVED node exists
        List<CompareService.DiffEntry> entries = compareService.flatten(result);
        assertTrue(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.MOVED && e.relativePath().equals("dir2/moved.txt")), "Should have MOVED status");

        // Verify that the original REMOVED node is NOT present
        assertFalse(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.REMOVED && e.relativePath().equals("dir1/moved.txt")), "Should NOT show up as REMOVED");
    }

    @Test
    void testFileMoveModifiedNoRemovedNode() throws IOException {
        Path left = tempDir.resolve("left_move_mod");
        Path right = tempDir.resolve("right_move_mod");
        Files.createDirectories(left.resolve("dir1"));
        Files.createDirectories(right.resolve("dir2"));

        Files.writeString(left.resolve("dir1/moved.txt"), "content A");
        Files.writeString(right.resolve("dir2/moved.txt"), "content B");

        DiffNode result = compareService.compareDirectories(left, right);

        // Verify the MOVED_MODIFIED node exists
        List<CompareService.DiffEntry> entries = compareService.flatten(result);
        assertTrue(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.MOVED_MODIFIED && e.relativePath().equals("dir2/moved.txt")), "Should have MOVED_MODIFIED status");

        // Verify that the original REMOVED node is NOT present
        assertFalse(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.REMOVED && e.relativePath().equals("dir1/moved.txt")), "Should NOT show up as REMOVED");
    }

    @Test
    void testFileMove() throws IOException {
        Path left = tempDir.resolve("left");
        Path right = tempDir.resolve("right");
        Files.createDirectories(left.resolve("old-loc"));
        Files.createDirectories(right.resolve("new-loc"));

        Files.writeString(left.resolve("old-loc/MyClass.java"), "public class MyClass {}");
        Files.writeString(right.resolve("new-loc/MyClass.java"), "public class MyClass {}");

        DiffNode result = compareService.compareDirectories(left, right);

        // Find the moved file in the result tree
        DiffNode newLoc = result.getChildren().stream().filter(n -> n.getName().equals("new-loc")).findFirst().orElseThrow();
        DiffNode movedFile = newLoc.getChildren().get(0);

        assertEquals("MyClass.java", movedFile.getName());
        assertEquals(DiffNode.DiffStatus.MOVED, movedFile.getStatus());
        assertEquals("old-loc/MyClass.java", movedFile.getSourcePath());
        
        // The old location should be EMPTY (REMOVED node removed)
        DiffNode oldLoc = result.getChildren().stream().filter(n -> n.getName().equals("old-loc")).findFirst().orElseThrow();
        assertTrue(oldLoc.getChildren().isEmpty(), "REMOVED node should have been removed from the tree");
    }

    @Test
    void testFileMoveModified() throws IOException {
        Path left = tempDir.resolve("left2");
        Path right = tempDir.resolve("right2");
        Files.createDirectories(left.resolve("dir1"));
        Files.createDirectories(right.resolve("dir2"));

        Files.writeString(left.resolve("dir1/App.java"), "public class App {}");
        Files.writeString(right.resolve("dir2/App.java"), "public class App { // modified }");

        DiffNode result = compareService.compareDirectories(left, right);

        DiffNode dir2 = result.getChildren().stream().filter(n -> n.getName().equals("dir2")).findFirst().orElseThrow();
        DiffNode movedFile = dir2.getChildren().get(0);

        assertEquals("App.java", movedFile.getName());
        assertEquals(DiffNode.DiffStatus.MOVED_MODIFIED, movedFile.getStatus());
        assertEquals("dir1/App.java", movedFile.getSourcePath());
        assertTrue(movedFile.getModified() > 0 || movedFile.getAdded() > 0 || movedFile.getRemoved() > 0);
        assertTrue(movedFile.getPercentage() > 0);
    }
    @Test
    void testJavaMoveWithDifferentPackages() throws IOException {
        Path left = tempDir.resolve("left_pkg");
        Path right = tempDir.resolve("right_pkg");
        Files.createDirectories(left.resolve("com/fisglobal/base"));
        Files.createDirectories(right.resolve("com/fisglobal/bean"));

        Files.writeString(left.resolve("com/fisglobal/base/Application.java"), "package com.fisglobal.base;\npublic class Application {}");
        Files.writeString(right.resolve("com/fisglobal/bean/Application.java"), "package com.fisglobal.bean;\npublic class Application {}");

        DiffNode result = compareService.compareDirectories(left, right);
        List<CompareService.DiffEntry> entries = compareService.flatten(result);

        // They should NOT be matched as MOVED because the packages are different
        assertFalse(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.MOVED), "Should NOT be matched as MOVED");
        assertFalse(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.MOVED_MODIFIED), "Should NOT be matched as MOVED_MODIFIED");
        
        assertTrue(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.REMOVED && e.relativePath().equals("com/fisglobal/base/Application.java")));
        assertTrue(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.ADDED && e.relativePath().equals("com/fisglobal/bean/Application.java")));
    }

    @Test
    void testJavaMoveWithSamePackage() throws IOException {
        Path left = tempDir.resolve("left_same_pkg");
        Path right = tempDir.resolve("right_same_pkg");
        Files.createDirectories(left.resolve("JavaSource/com/fisglobal/base"));
        Files.createDirectories(right.resolve("src/main/com/fisglobal/base"));

        Files.writeString(left.resolve("JavaSource/com/fisglobal/base/Application.java"), "package com.fisglobal.base;\npublic class Application {}");
        Files.writeString(right.resolve("src/main/com/fisglobal/base/Application.java"), "package com.fisglobal.base;\npublic class Application {}");

        DiffNode result = compareService.compareDirectories(left, right);
        List<CompareService.DiffEntry> entries = compareService.flatten(result);

        // They SHOULD be matched as MOVED because the package is the same
        assertTrue(entries.stream().anyMatch(e -> e.status() == DiffNode.DiffStatus.MOVED && e.relativePath().equals("src/main/com/fisglobal/base/Application.java")), "Should be matched as MOVED");
    }
}
