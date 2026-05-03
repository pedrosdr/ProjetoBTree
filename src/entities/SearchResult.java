package entities;

public class SearchResult {
    private int p;
    private int i;
    private boolean found;

    public SearchResult(int p, int i, boolean found) {
        this.p = p;
        this.i = i;
        this.found = found;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public boolean getFound() {
        return found;
    }

    public void setFound(boolean found) {
        this.found = found;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "p=" + p +
                ", i=" + i +
                ", j=" + found +
                '}';
    }
}
