package entities;

public class SplitResult {
    private final Node p;
    private final int promotedKey;
    private final int promotedRecordAddress;
    private final Node q;

    public SplitResult(Node p, int promotedKey, int promotedRecordAddress, Node q) {
        this.p = p;
        this.promotedKey = promotedKey;
        this.promotedRecordAddress = promotedRecordAddress;
        this.q = q;
    }

    public Node getP() {
        return p;
    }

    public int getPromotedKey() {
        return promotedKey;
    }

    public int getPromotedRecordAddress() {
        return promotedRecordAddress;
    }

    public Node getQ() {
        return q;
    }
}