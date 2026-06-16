package entities;

public class SearchResult {
    private int p;
    private int i;
    private boolean found;
    private int recordAddress;

    public SearchResult(int p, int i, boolean found) {
        this(p, i, found, 0);
    }

    public SearchResult(int p, int i, boolean found, int recordAddress) {
        this.p = p;
        this.i = i;
        this.found = found;
        this.recordAddress = recordAddress;
    }

    public int getP() {
        return p;
    }

    public int getI() {
        return i;
    }

    public boolean getFound() {
        return found;
    }

    public int getRecordAddress() {
        return recordAddress;
    }
}