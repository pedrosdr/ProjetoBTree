package entities;

public class SearchResult {
    private int p;
    private int i;
    private boolean j;

    public SearchResult(int p, int i, boolean j) {
        this.p = p;
        this.i = i;
        this.j = j;
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

    public boolean getJ() {
        return j;
    }

    public void setJ(boolean j) {
        this.j = j;
    }

    @Override
    public String toString() {
        return "SearchResult{" +
                "p=" + p +
                ", i=" + i +
                ", j=" + j +
                '}';
    }
}
