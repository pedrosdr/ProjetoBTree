package entities;

public class PathEntry {
    private final int id;
    private final Node node;

    public PathEntry(int id, Node node) {
        this.id = id;
        this.node = node;
    }

    public int getId() {
        return id;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public String toString() {
        return "PathEntry{" + getId() + ", " + getNode() + "}";
    }
}