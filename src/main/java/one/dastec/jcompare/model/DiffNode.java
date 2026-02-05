package one.dastec.jcompare.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class DiffNode {
    private String name;
    private boolean isDirectory;
    private DiffStatus status; // ADDED, REMOVED, MODIFIED, IDENTICAL
    private int added;
    private int removed;
    private int modified;
    private double percentage;
    private String relativePath;
    private String sourcePath;
    @Builder.Default
    private List<DiffNode> children = new ArrayList<>();

    public enum DiffStatus {
        ADDED, REMOVED, MODIFIED, IDENTICAL, MOVED
    }
}
