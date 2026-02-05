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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class CompareControllerTest {

    @Autowired
    private CompareController compareController;

    @MockitoBean
    private CompareService compareService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Test
    void testExport() throws Exception {
        DiffNode diffNode = DiffNode.builder()
                .name("root")
                .status(DiffNode.DiffStatus.MOVED)
                .relativePath("dest.txt")
                .sourcePath("source.txt")
                .isDirectory(false)
                .build();

        when(compareService.compareDirectories(any(), any())).thenReturn(diffNode);
        when(compareService.flatten(any())).thenReturn(List.of(
                new CompareService.DiffEntry("root/dest.txt", false, DiffNode.DiffStatus.MOVED, "dest.txt", 0, 0, 0, 0.0, "source.txt")
        ));

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        mockMvc.perform(get("/export")
                        .param("leftPath", "/tmp/a")
                        .param("rightPath", "/tmp/b")
                        .param("typeFilter", "all")
                        .param("statusFilter", "all"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("text/csv"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"jcompare_export.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Destination Path,Source Path,Type,Status,Diff %,Added,Modified,Deleted")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"root/dest.txt\",\"source.txt\",\"File\",\"MOVED\",\"-\",\"-\",\"-\",\"-\"")));
    }

    @Test
    void testExportWithFilters() throws Exception {
        DiffNode diffNode = DiffNode.builder().name("root").build();
        when(compareService.compareDirectories(any(), any())).thenReturn(diffNode);
        when(compareService.flatten(any())).thenReturn(List.of(
                new CompareService.DiffEntry("file.java", false, DiffNode.DiffStatus.ADDED, "file.java", 10, 0, 0, 100.0, null),
                new CompareService.DiffEntry("other.txt", false, DiffNode.DiffStatus.REMOVED, "other.txt", 0, 5, 0, 100.0, null)
        ));

        MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Filter by type: java
        mockMvc.perform(get("/export")
                        .param("leftPath", "/tmp/a")
                        .param("rightPath", "/tmp/b")
                        .param("typeFilter", "java")
                        .param("statusFilter", "all"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("file.java")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("other.txt"))));

        // Filter by status: removed
        mockMvc.perform(get("/export")
                        .param("leftPath", "/tmp/a")
                        .param("rightPath", "/tmp/b")
                        .param("typeFilter", "all")
                        .param("statusFilter", "removed"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("other.txt")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("file.java"))));
    }

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
        String view = compareController.fileDiff("/tmp/a", "/tmp/b", "file.java", null, model);
        
        assertEquals("fileDiff", view);
        assertEquals("/tmp/a", model.getAttribute("leftPath"));
        assertEquals("/tmp/b", model.getAttribute("rightPath"));
        assertEquals("file.java", model.getAttribute("relativePath"));
        assertEquals("file.java", model.getAttribute("fileName"));
        assertNotNull(model.getAttribute("fileDiff"));
    }
}
