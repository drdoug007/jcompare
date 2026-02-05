package one.dastec.jcompare.controller;

import one.dastec.jcompare.model.DiffNode;
import one.dastec.jcompare.service.CompareService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class CompareControllerTest {

    @Autowired
    private CompareController compareController;

    @MockitoBean
    private CompareService compareService;

    @Test
    void testIndexWithoutParams() throws IOException {
        Model model = new ConcurrentModel();
        String view = compareController.index(null, null, "tree", model);
        
        assertEquals("index", view);
        assertEquals("", model.getAttribute("leftPath"));
        assertEquals("", model.getAttribute("rightPath"));
        assertNull(model.getAttribute("diffResult"));
    }

    @Test
    void testIndexWithPaths() throws IOException {
        DiffNode diffNode = DiffNode.builder()
                .name("root")
                .status(DiffNode.DiffStatus.IDENTICAL)
                .build();
        
        when(compareService.compareDirectories(any(Path.class), any(Path.class))).thenReturn(diffNode);

        Model model = new ConcurrentModel();
        String view = compareController.index("/tmp/a", "/tmp/b", "tree", model);
        
        assertEquals("index", view);
        assertEquals("/tmp/a", model.getAttribute("leftPath"));
        assertEquals("/tmp/b", model.getAttribute("rightPath"));
        assertEquals(diffNode, model.getAttribute("diffResult"));
    }

    @Test
    void testIndexWithTableView() throws IOException {
        DiffNode diffNode = DiffNode.builder()
                .name("root")
                .status(DiffNode.DiffStatus.IDENTICAL)
                .build();
        
        when(compareService.compareDirectories(any(Path.class), any(Path.class))).thenReturn(diffNode);

        Model model = new ConcurrentModel();
        String view = compareController.index("/tmp/a", "/tmp/b", "table", model);
        
        assertEquals("index", view);
        assertEquals("table", model.getAttribute("viewType"));
        assertNotNull(model.getAttribute("tableResult"));
    }

    @Test
    void testLs() throws IOException {
        var items = compareController.ls("");
        assertNotNull(items);
        // Should contain home directory items
    }

    @Test
    void testLsWithNonExistentPath() throws IOException {
        var items = compareController.ls("/non-existent-path-12345");
        assertTrue(items.isEmpty());
    }

    @Test
    void testLsIgnoresDirectories(@TempDir Path tempDir) throws IOException {
        Files.createDirectories(tempDir.resolve("target"));
        Files.createDirectories(tempDir.resolve("src"));
        Files.createDirectories(tempDir.resolve(".git"));
        
        // Mock the service to return true for ignored paths
        when(compareService.isIgnored(any(Path.class))).thenAnswer(invocation -> {
            Path path = invocation.getArgument(0);
            String name = path.getFileName().toString();
            return name.equals("target") || name.equals(".git");
        });

        var items = compareController.ls(tempDir.toString());
        assertEquals(1, items.size());
        assertEquals("src", items.get(0).name());
    }

    @Test
    void testFileDiff() throws IOException {
        when(compareService.compareFiles(any(), any())).thenReturn(new CompareService.FileDiff(List.of(), 0, 0, 0, 0.0));
        
        Model model = new ConcurrentModel();
        String view = compareController.fileDiff("/tmp/a", "/tmp/b", "file.java", model);
        
        assertEquals("fileDiff", view);
        assertEquals("/tmp/a", model.getAttribute("leftPath"));
        assertEquals("/tmp/b", model.getAttribute("rightPath"));
        assertEquals("file.java", model.getAttribute("relativePath"));
        assertEquals("file.java", model.getAttribute("fileName"));
        assertNotNull(model.getAttribute("fileDiff"));
    }
}
