package entities;

public class KAPair {
    // fields
    private int k;
    private int a;

    // constructors
    public KAPair(){}
    public KAPair(int k, int a) {
        this.k = k;
        this.a = a;
    }

    // properties
    public void setA(int a) {
        this.a = a;
    }

    public int getA() {
        return a;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getK() {
        return k;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        sb.append(k);
        sb.append(", ");
        sb.append(a);
        sb.append(")");
        return sb.toString();
    }
}
