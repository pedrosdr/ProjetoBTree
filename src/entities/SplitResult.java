package entities;

public class SplitResult {
    private final Node p;
    private final int promotedKey;
    private final Node q;

    public SplitResult(Node p, int promotedKey, Node q) {
        this.p = p;
        this.promotedKey = promotedKey;
        this.q = q;
    }

    public Node getP() {
        return p;
    }

    public int getPromotedKey() {
        return promotedKey;
    }

    public Node getQ() {
        return q;
    }
}