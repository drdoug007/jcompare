package one.dastec.jcompare.controller;

import one.dastec.jcompare.model.DiffNode;
import one.dastec.jcompare.service.CompareService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Controller
public class CompareController {

    private final CompareService compareService;

    public CompareController(CompareService compareService) {
        this.compareService = compareService;
    }

    @GetMapping("/")
    public String index(
            @RequestParam(required = false) String leftPath,
            @RequestParam(required = false) String rightPath,
            @RequestParam(required = false, defaultValue = "tree") String viewType,
            Model model) throws IOException {

        model.addAttribute("leftPath", leftPath != null ? leftPath : "");
        model.addAttribute("rightPath", rightPath != null ? rightPath : "");
        model.addAttribute("viewType", viewType);

        if (leftPath != null && !leftPath.isEmpty() && rightPath != null && !rightPath.isEmpty()) {
            Path left = Paths.get(leftPath);
            Path right = Paths.get(rightPath);
            DiffNode diffResult = compareService.compareDirectories(left, right);
            model.addAttribute("diffResult", diffResult);
            
            if ("table".equals(viewType)) {
                List<CompareService.DiffEntry> tableResult = compareService.flatten(diffResult);
                model.addAttribute("tableResult", tableResult);
            }
        } else {
            model.addAttribute("diffResult", null);
        }

        return "index";
    }

    @GetMapping("/api/ls")
    @org.springframework.web.bind.annotation.ResponseBody
    public List<FileItem> ls(@RequestParam(required = false, defaultValue = "") String path) throws IOException {
        Path root = path.isEmpty() ? Paths.get(System.getProperty("user.home")) : Paths.get(path);
        if (!java.nio.file.Files.exists(root) || !java.nio.file.Files.isDirectory(root)) {
            return List.of();
        }
        
        // Use CompareService to determine if a path is ignored
        try (var stream = java.nio.file.Files.list(root)) {
            return stream
                    .filter(java.nio.file.Files::isDirectory)
                    .filter(p -> !isIgnored(p))
                    .map(p -> new FileItem(p.getFileName().toString(), p.toAbsolutePath().toString(), true))
                    .sorted(java.util.Comparator.comparing(FileItem::name))
                    .toList();
        }
    }

    private boolean isIgnored(Path path) {
        // Reflection-like hack to use the service's private method or just expose it
        // Better yet, just call a method on compareService.
        // For now, I'll update CompareService to have a public isIgnored.
        return compareService.isIgnored(path);
    }

    @GetMapping("/diff")
    public String fileDiff(
            @RequestParam String leftPath,
            @RequestParam String rightPath,
            @RequestParam String relativePath,
            @RequestParam(required = false) String sourcePath,
            Model model) throws IOException {
        Path left;
        if (sourcePath != null && !sourcePath.isEmpty()) {
            left = Paths.get(leftPath).resolve(sourcePath);
        } else {
            left = Paths.get(leftPath).resolve(relativePath);
        }
        Path right = Paths.get(rightPath).resolve(relativePath);
        
        CompareService.FileDiff fileDiff = compareService.compareFiles(
                java.nio.file.Files.exists(left) ? left : null,
                java.nio.file.Files.exists(right) ? right : null
        );
        
        model.addAttribute("leftPath", leftPath);
        model.addAttribute("rightPath", rightPath);
        model.addAttribute("relativePath", relativePath);
        model.addAttribute("fileDiff", fileDiff);
        model.addAttribute("fileName", Paths.get(relativePath).getFileName().toString());
        
        return "fileDiff";
    }

    @GetMapping("/export")
    public void export(
            @RequestParam String leftPath,
            @RequestParam String rightPath,
            @RequestParam(required = false, defaultValue = "all") String typeFilter,
            @RequestParam(required = false, defaultValue = "all") String statusFilter,
            jakarta.servlet.http.HttpServletResponse response) throws IOException {

        Path left = Paths.get(leftPath);
        Path right = Paths.get(rightPath);
        DiffNode diffResult = compareService.compareDirectories(left, right);
        List<CompareService.DiffEntry> tableResult = compareService.flatten(diffResult);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"jcompare_export.csv\"");

        try (java.io.PrintWriter writer = response.getWriter()) {
            // Header
            writer.println("Destination Path,Source Path,Type,Status,Diff %,Added,Modified,Deleted");

            for (CompareService.DiffEntry entry : tableResult) {
                // Apply filters if they were provided (to match UI view)
                if (!"all".equals(typeFilter)) {
                    String type = entry.isDirectory() ? "directory" : (entry.path().endsWith(".java") ? "java" : (entry.path().endsWith(".xml") ? "xml" : (entry.path().endsWith(".json") ? "json" : (entry.path().endsWith(".yaml") || entry.path().endsWith(".yml") ? "yaml" : (entry.path().endsWith(".properties") ? "props" : "file")))));
                    if (!typeFilter.equals(type)) continue;
                }
                if (!"all".equals(statusFilter)) {
                    if (!statusFilter.equals(entry.status().name().toLowerCase())) continue;
                }

                String destination = entry.path();
                String source = entry.sourcePath() != null ? entry.sourcePath() : "";
                String type = entry.isDirectory() ? "Directory" : "File";
                String status = entry.status().name();
                String diffPct = entry.isDirectory() ? "-" : (entry.status() == DiffNode.DiffStatus.MOVED ? "-" : String.format("%.1f%%", entry.percentage()));
                String added = (entry.isDirectory() || entry.status() == DiffNode.DiffStatus.MOVED) ? "-" : String.valueOf(entry.added());
                String modified = (entry.isDirectory() || entry.status() == DiffNode.DiffStatus.MOVED) ? "-" : String.valueOf(entry.modified());
                String deleted = (entry.isDirectory() || entry.status() == DiffNode.DiffStatus.MOVED) ? "-" : String.valueOf(entry.removed());

                writer.println(String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                        destination.replace("\"", "\"\""),
                        source.replace("\"", "\"\""),
                        type,
                        status,
                        diffPct,
                        added,
                        modified,
                        deleted));
            }
        }
    }

    public record FileItem(String name, String path, boolean isDirectory) {}
}
